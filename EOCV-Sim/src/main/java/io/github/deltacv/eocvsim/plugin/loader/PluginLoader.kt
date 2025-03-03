/*
 * Copyright (c) 2024 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.github.deltacv.eocvsim.plugin.loader

import com.github.serivesmejia.eocvsim.EOCVSim
import com.github.serivesmejia.eocvsim.config.ConfigLoader
import com.github.serivesmejia.eocvsim.gui.dialog.AppendDelegate
import com.github.serivesmejia.eocvsim.gui.dialog.PluginOutput
import com.github.serivesmejia.eocvsim.util.event.EventHandler
import com.github.serivesmejia.eocvsim.util.extension.hashString
import com.github.serivesmejia.eocvsim.util.extension.plus
import com.github.serivesmejia.eocvsim.util.loggerForThis
import com.moandjiezana.toml.Toml
import io.github.deltacv.common.util.ParsedVersion
import io.github.deltacv.eocvsim.plugin.EOCVSimPlugin
import io.github.deltacv.eocvsim.plugin.security.PluginSignatureVerifier
import io.github.deltacv.eocvsim.sandbox.nio.SandboxFileSystem
import net.lingala.zip4j.ZipFile
import java.io.File

enum class PluginSource {
    REPOSITORY,
    FILE
}

class PluginParser(pluginToml: Toml) {
    val pluginName = pluginToml.getString("name")?.trim() ?: throw InvalidPluginException("No name in plugin.toml")
    val pluginVersion = pluginToml.getString("version")?.trim() ?: throw InvalidPluginException("No version in plugin.toml")

    val pluginAuthor = pluginToml.getString("author")?.trim() ?: throw InvalidPluginException("No author in plugin.toml")
    val pluginAuthorEmail = pluginToml.getString("author-email", "")?.trim()

    val pluginMain = pluginToml.getString("main")?.trim() ?: throw InvalidPluginException("No main in plugin.toml")

    val pluginDescription = pluginToml.getString("description", "")?.trim()

    /**
     * Get the hash of the plugin based off the plugin name and author
     * @return the hash
     */
    fun hash() = "${pluginName}${PluginOutput.SPECIAL}${pluginAuthor}".hashString
}

/**
 * Loads a plugin from a jar file
 * @param pluginFile the jar file of the plugin
 * @param eocvSim the EOCV-Sim instance
 */
class PluginLoader(
    val pluginFile: File,
    val classpath: List<File>,
    val pluginSource: PluginSource,
    val eocvSim: EOCVSim,
    val appender: AppendDelegate
) {

    val logger by loggerForThis()

    var loaded = false
        private set

    var enabled = false
        private set

    val pluginClassLoader: PluginClassLoader

    var shouldEnable: Boolean
        get() {
            return eocvSim.config.flags.getOrDefault(hash(), true)
        }
        set(value) {
            eocvSim.config.flags[hash()] = value
            eocvSim.configManager.saveToFile()
        }

    lateinit var pluginToml: Toml
        private set

    lateinit var pluginName: String
        private set
    lateinit var pluginVersion: String
        private set

    lateinit var pluginDescription: String
        private set

    lateinit var pluginAuthor: String
        private set
    lateinit var pluginAuthorEmail: String
        private set

    lateinit var pluginClass: Class<*>
        private set
    lateinit var plugin: EOCVSimPlugin
        private set

    /**
     * The file system for the plugin
     */
    lateinit var fileSystem: SandboxFileSystem
        private set

    /**
     * The signature of the plugin, issued by a verified authority
     */
    val signature by lazy { PluginSignatureVerifier.verify(pluginFile) }

    val fileSystemZip by lazy { PluginManager.FILESYSTEMS_FOLDER + File.separator + "${hash()}-fs" }
    val fileSystemZipPath by lazy { fileSystemZip.toPath() }

    /**
     * Whether the plugin has super access (full system access)
     */
    val hasSuperAccess get() = eocvSim.pluginManager.superAccessDaemonClient.checkAccess(pluginFile)

    init {
        pluginClassLoader = PluginClassLoader(
            pluginFile,
            classpath
        ) {
            PluginContext(eocvSim, fileSystem, this)
        }
    }

    /**
     * Fetch the plugin info from the plugin.toml file
     * Fills the pluginName, pluginVersion, pluginAuthor and pluginAuthorEmail fields
     */
    fun fetchInfoFromToml() {
        if(::pluginToml.isInitialized) return

        pluginToml = Toml().read(pluginClassLoader.getResourceAsStream("plugin.toml")
            ?: throw InvalidPluginException("No plugin.toml in the jar file")
        )

        val parser = PluginParser(pluginToml)

        pluginName = parser.pluginName
        pluginVersion = parser.pluginVersion
        pluginAuthor = parser.pluginAuthor
        pluginAuthorEmail = parser.pluginAuthorEmail ?: ""
        pluginDescription = parser.pluginDescription ?: ""
    }

    /**
     * Load the plugin from the jar file
     * @throws InvalidPluginException if the plugin.toml file is not found
     * @throws UnsupportedPluginException if the plugin requests an api version higher than the current one
     */
    fun load() {
        if(loaded) return

        fetchInfoFromToml()

        if(!shouldEnable) {
            appender.appendln("${PluginOutput.SPECIAL_SILENT}Plugin $pluginName v$pluginVersion is disabled")
            return
        }

        appender.appendln("${PluginOutput.SPECIAL_SILENT}Loading plugin $pluginName v$pluginVersion by $pluginAuthor from ${pluginSource.name}")

        signature

        setupFs()

        if(pluginToml.contains("api-version") || pluginToml.contains("min-api-version")) {
            // default to api-version if min-api-version is not present
            val apiVersionKey = if(pluginToml.contains("api-version")) "api-version" else "min-api-version"
            val parsedVersion = ParsedVersion(pluginToml.getString(apiVersionKey))

            if(parsedVersion > EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires a minimum api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin $pluginName requests min api version of v${parsedVersion}")
        }

        if(pluginToml.contains("max-api-version")) {
            val parsedVersion = ParsedVersion(pluginToml.getString("max-api-version"))

            if(parsedVersion < EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires a max api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin $pluginName requests max api version of v${parsedVersion}")
        }

        if(pluginToml.contains("exact-api-version")) {
            val parsedVersion = ParsedVersion(pluginToml.getString("exact-api-version"))

            if(parsedVersion != EOCVSim.PARSED_VERSION)
                throw UnsupportedPluginException("Plugin requires an exact api version of v${parsedVersion}, EOCV-Sim is currently running at v${EOCVSim.PARSED_VERSION}")

            logger.info("Plugin $pluginName requests exact api version of v${parsedVersion}")
        }

        if(pluginToml.getBoolean("super-access", false)) {
            requestSuperAccess(pluginToml.getString("super-access-reason", ""))
        }

        pluginClass = pluginClassLoader.loadClassStrict(pluginToml.getString("main"))
        plugin = pluginClass.getConstructor().newInstance() as EOCVSimPlugin

        plugin.onLoad()

        loaded = true
    }

    private fun setupFs() {
        if(!fileSystemZip.exists()) {
            val zip = ZipFile(fileSystemZip) // kinda wack but uh, yeah...
            zip.addFile(ConfigLoader.CONFIG_SAVEFILE)
            zip.removeFile(ConfigLoader.CONFIG_SAVEFILE.name)
            zip.close()
        }

        fileSystem = SandboxFileSystem(this)
    }

    /**
     * Enable the plugin
     */
    fun enable() {
        if(enabled || !loaded) return

        if(!shouldEnable) return

        appender.appendln("${PluginOutput.SPECIAL_SILENT}Enabling plugin $pluginName v$pluginVersion")

        plugin.enabled = true
        plugin.onEnable()

        enabled = true
    }

    /**
     * Disable the plugin
     */
    fun disable() {
        if(!enabled || !loaded) return

        appender.appendln("${PluginOutput.SPECIAL_SILENT}Disabling plugin $pluginName v$pluginVersion")

        plugin.enabled = false
        plugin.onDisable()

        kill()
    }

    /**
     * Kill the plugin
     * This will close the file system and ban the class loader
     * @see EventHandler.banClassLoader
     */
    fun kill() {
        if(!loaded) return
        fileSystem.close()
        enabled = false
        EventHandler.banClassLoader(pluginClassLoader)

        pluginClassLoader.close()
    }

    /**
     * Request super access for the plugin
     * @param reason the reason for requesting super access
     */
    fun requestSuperAccess(reason: String): Boolean {
        return eocvSim.pluginManager.requestSuperAccessFor(this, reason)
    }

    /**
     * Get the hash of the plugin based off the plugin name and author
     * @return the hash
     */
    fun hash() = "${pluginName}${PluginOutput.SPECIAL}${pluginAuthor}".hashString

}