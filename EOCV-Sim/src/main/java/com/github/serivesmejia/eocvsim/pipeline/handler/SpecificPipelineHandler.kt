/*
 * Copyright (c) 2023 Sebastian Erives
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

package com.github.serivesmejia.eocvsim.pipeline.handler

import org.firstinspires.ftc.robotcore.external.Telemetry
import org.openftc.easyopencv.OpenCvPipeline

abstract class SpecificPipelineHandler<P: OpenCvPipeline>(
        val typeChecker: (OpenCvPipeline) -> Boolean
) : PipelineHandler {

    var pipeline: P? = null
        private set

    var telemetry: Telemetry? = null
        private set

    @Suppress("UNCHECKED_CAST")
    override fun onChange(beforePipeline: OpenCvPipeline?, newPipeline: OpenCvPipeline, telemetry: Telemetry) {
        if(typeChecker(newPipeline)) {
            this.pipeline = newPipeline as P
            this.telemetry = telemetry
        } else {
            this.pipeline = null
            this.telemetry = null // "don't get paid enough to handle this shit"
                                  // - OpModePipelineHandler, probably
        }
    }

}