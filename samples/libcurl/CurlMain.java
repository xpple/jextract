/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle nor the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.lang.foreign.Arena;
import java.lang.foreign.SegmentAllocator;
import static java.lang.foreign.MemorySegment.NULL;
import static org.jextract.curl_h.*;
import org.jextract.*;

public class CurlMain {
   public static void main(String[] args) {
       var urlStr = args[0];
       curl_global_init(CURL_GLOBAL_DEFAULT());
       var curl = curl_easy_init();
       if(!curl.equals(NULL)) {
           try (var arena = Arena.ofConfined()) {
               var url = arena.allocateFrom(urlStr);
               curl_easy_setopt.
                   makeInvoker(C_LONG_LONG).
                   apply(curl, CURLOPT_URL(), url.address());
               int res = curl_easy_perform(curl);
               if (res != CURLE_OK()) {
                   String error = curl_easy_strerror(res).getString(0);
                   System.out.println("Curl error: " + error);
                   curl_easy_cleanup(curl);
               }
           }
       }
       curl_global_cleanup();
   }
}

