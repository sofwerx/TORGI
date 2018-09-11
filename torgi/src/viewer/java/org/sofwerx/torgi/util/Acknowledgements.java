package org.sofwerx.torgi.util;

public class Acknowledgements {
    private final static String[] CREDITS = {
            "Charts generated from <a href=\"https://github.com/PhilJay/MPAndroidChart\">MPAndroidChart, Phillip Jahoda</a> licensed under <a href=\"http://www.apache.org/licenses/LICENSE-2.0\">Apache License, Version 2.0</a>",
            "GNSS icon derived from <a href=\"https://thenounproject.com/search/?q=satellite&i=1556189\">The Noun Project, Weltenraser</a> licensed under <a href=\"https://creativecommons.org/licenses/by/3.0/us/legalcode\">Creative Commons</a>"
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
                    "<br>     ======================================"
    };
}
