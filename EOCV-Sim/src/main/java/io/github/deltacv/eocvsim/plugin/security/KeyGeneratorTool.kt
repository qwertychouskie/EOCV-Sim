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

package io.github.deltacv.eocvsim.plugin.security

import java.io.File
import java.io.FileWriter
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.util.Base64

fun main() {
    // Generate RSA key pair
    val keyPair: KeyPair = generateKeyPair()

    // Save keys to files
    saveKeyToFile("private_key.pem", keyPair.private)
    saveKeyToFile("public_key.pem", keyPair.public)

    println("Keys generated and saved to files.")
}

fun generateKeyPair(): KeyPair {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048) // Use 2048 bits for key size
    return keyPairGenerator.generateKeyPair()
}

fun saveKeyToFile(filename: String, key: java.security.Key) {
    val encodedKey = Base64.getEncoder().encodeToString(key.encoded)
    val keyType = if (key is PrivateKey) "PRIVATE" else "PUBLIC"

    val pemFormat = "-----BEGIN ${keyType} KEY-----\n$encodedKey\n-----END ${keyType} KEY-----"

    FileWriter(File(filename)).use { writer ->
        writer.write(pemFormat)
    }
}