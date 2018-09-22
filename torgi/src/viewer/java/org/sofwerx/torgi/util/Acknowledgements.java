package org.sofwerx.torgi.util;

public class Acknowledgements {
    private final static String[] CREDITS = {
            "Charts generated from <a href=\"https://github.com/PhilJay/MPAndroidChart\">MPAndroidChart, Phillip Jahoda</a> licensed under <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License, Version 2.0</a>",
            "GNSS icon derived from <a href=\"https://thenounproject.com/search/?q=gps&i=1112816\">The Noun Project, Dinosoft Labs</a> licensed under <a href=\"https://creativecommons.org/licenses/by/3.0/us/legalcode\">Creative Commons</a>",
            "<a href=\"https://github.com/NanoHttpd/nanohttpd\">NanoHttpd</a> licensed under <a href=\"https://github.com/NanoHttpd/nanohttpd/blob/master/LICENSE.md\">BSD-3 Clause \"New\" or \"Revised\" License</a>"
    };

    public final static String getCredits() {
        StringBuffer out = new StringBuffer();
        boolean first = true;
        for (String credit:CREDITS) {
            if (first)
                first = false;
            else
                out.append("<br>");
            out.append("• ");
            out.append(credit);
        }
        return out.toString();
    }

    public final static String getLicenses() {
        StringBuffer out = new StringBuffer();
        boolean first = true;
        for (String license:LICENSES) {
            if (first)
                first = false;
            else
                out.append("<br><br>");
            out.append(license);
        }
        return out.toString();
    }

    public final static String[] LICENSES = {
            "Some code derived from:<br><b>MPAndroidChart</b><br>"+
                    "<br>     ======================================<br>" +
                    "<br>   Licensed under the Apache License, Version 2.0 (the \"License\");<br>" +
                    "   you may not use this file except in compliance with the License.<br>" +
                    "   You may obtain a copy of the License at<br>" +
                    "       http://www.apache.org/licenses/LICENSE-2.0<br>" +
                    "   Unless required by applicable law or agreed to in writing, software<br>" +
                    "   distributed under the License is distributed on an \"AS IS\" BASIS,<br>" +
                    "   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.<br>" +
                    "   See the License for the specific language governing permissions and<br>" +
                    "   limitations under the License.<br>" +
                    //"<br>     ========================================================="
                    "<br>     ======================================",
            "NanoHttpd-Webserver, Copyright (C) 2012 - 2015 nanohttpd\n" +
                    "<br>Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:\n" +
                    "<br><br>1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.\n" +
                    "<br><br>2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.\n" +
                    "<br><br>3. Neither the name of the nanohttpd nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.<br><br>\n" +
                    "THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS \\\"AS IS\\\" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE."
    };
}
