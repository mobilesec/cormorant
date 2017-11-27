/**
 * Copyright 2016 - 2017
 *
 * Daniel Hintze <daniel.hintze@fhdw.de>
 * Sebastian Scholz <sebastian.scholz@fhdw.de>
 * Rainhard D. Findling <rainhard.findling@fh-hagenberg.at>
 * Muhammad Muaaz <muhammad.muaaz@usmile.at>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fhdw.locationriskplugin;


import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class MacroLocationRiskLevel {

    private static final String LOG_TAG = MacroLocationRiskLevel.class.getSimpleName();

    public static int getRiskLevelForCountry(String countryCode) {
        Log.d(LOG_TAG, "computing macro location risk for " + countryCode);

        return 0;
    }

    private static Map<String, Double> ROBBERIES = new HashMap<>();

    static {
        ROBBERIES.put("MM", 0.18);		//Myanmar
        ROBBERIES.put("LB", 0.36);		//Lebanon
        ROBBERIES.put("CR", 1095.64);		//Costa Rica
        ROBBERIES.put("AR", 1125.94);		//Argentina
        ROBBERIES.put("AL", 10.28);		//Albania
        ROBBERIES.put("BB", 100.57);		//Barbados
        ROBBERIES.put("US", 101.08);		//United States of America
        ROBBERIES.put("GE", 11.67);		//Georgia
        ROBBERIES.put("LU", 110.50);		//Luxembourg
        ROBBERIES.put("CM", 110.96);		//Cameroon
        ROBBERIES.put("SK", 12.54);		//Slovakia
        ROBBERIES.put("SI", 14.38);		//Slovenia
        ROBBERIES.put("TH", 14.69);		//The former Yugoslav Republic of Macedonia
        ROBBERIES.put("PR", 140.27);		//Puerto Rico
        ROBBERIES.put("MX", 141.95);		//Mexico
        ROBBERIES.put("DO", 144.20);		//Dominican Republic
        ROBBERIES.put("PT", 149.91);		//Portugal
        ROBBERIES.put("IS", 15.58);		//Iceland
        ROBBERIES.put("ES", 153.17);		//Spain
        ROBBERIES.put("FR", 177.93);		//France
        ROBBERIES.put("GT", 19.44);		//Guatemala
        ROBBERIES.put("HU", 19.76);		//Hungary
        ROBBERIES.put("BE", 195.12);		//Belgium
        ROBBERIES.put("TT", 197.27);		//Trinidad and Tobago
        ROBBERIES.put("CO", 197.55);		//Colombia
        ROBBERIES.put("TH", 2.11);		//Thailand
        ROBBERIES.put("JP", 2.41);		//Japan
        ROBBERIES.put("AZ", 2.50);		//Azerbaijan
        ROBBERIES.put("LI", 2.68);		//Liechtenstein
        ROBBERIES.put("TZ", 20.09);		//United Republic of Tanzania
        ROBBERIES.put("NO", 20.65);		//Norway
        ROBBERIES.put("CN", 20.76);		//China, Macao Special Administrative Region
        ROBBERIES.put("GD", 22.57);		//Grenada
        ROBBERIES.put("KR", 22.93);		//Kosovo under UNSCR 1244
        ROBBERIES.put("MN", 23.57);		//Mongolia
        ROBBERIES.put("CZ", 24.16);		//Czechia
        ROBBERIES.put("PL", 24.45);		//Poland
        ROBBERIES.put("BY", 25.89);		//Belarus
        ROBBERIES.put("PE", 250.54);		//Peru
        ROBBERIES.put("PA", 253.08);		//Panama
        ROBBERIES.put("BA", 26.85);		//Bosnia and Herzegovina
        ROBBERIES.put("EE", 27.35);		//Estonia
        ROBBERIES.put("HR", 29.79);		//Croatia
        ROBBERIES.put("MD", 3.07);		//Republic of Moldova
        ROBBERIES.put("KR", 3.17);		//Republic of Korea
        ROBBERIES.put("ME", 30.07);		//Montenegro
        ROBBERIES.put("FI", 30.82);		//Finland
        ROBBERIES.put("PY", 309.88);		//Paraguay
        ROBBERIES.put("BG", 32.30);		//Bulgaria
        ROBBERIES.put("RO", 32.66);		//Romania
        ROBBERIES.put("GR", 34.54);		//Greece
        ROBBERIES.put("RS", 36.34);		//Serbia
        ROBBERIES.put("SG", 4.18);		//Singapore
        ROBBERIES.put("ID", 4.62);		//Indonesia
        ROBBERIES.put("LV", 40.67);		//Latvia
        ROBBERIES.put("AT", 40.87);		//Austria
        ROBBERIES.put("DK", 41.12);		//Denmark
        ROBBERIES.put("BI", 41.56);		//Burundi
        ROBBERIES.put("AU", 41.88);		//Australia
        ROBBERIES.put("PH", 44.11);		//Philippines
        ROBBERIES.put("NZ", 44.85);		//New Zealand
        ROBBERIES.put("MT", 45.48);		//Malta
        ROBBERIES.put("CH", 48.80);		//Switzerland
        ROBBERIES.put("BM", 52.90);		//Bermuda
        ROBBERIES.put("DZ", 53.97);		//Algeria
        ROBBERIES.put("RU", 54.19);		//Russian Federation
        ROBBERIES.put("UY", 543.26);		//Uruguay
        ROBBERIES.put("DE", 56.39);		//Germany
        ROBBERIES.put("IE", 56.62);		//Ireland
        ROBBERIES.put("LT", 57.87);		//Lithuania
        ROBBERIES.put("EC", 570.56);		//Ecuador
        ROBBERIES.put("CA", 58.79);		//Canada
        ROBBERIES.put("CL", 598.68);		//Chile
        ROBBERIES.put("KE", 6.71);		//Kenya
        ROBBERIES.put("NL", 61.18);		//Netherlands
        ROBBERIES.put("AE", 7.87);		//United Arab Emirates
        ROBBERIES.put("BW", 76.49);		//Botswana
        ROBBERIES.put("BI", 760.45);		//Cabo Verde
        ROBBERIES.put("SV", 77.48);		//El Salvador
        ROBBERIES.put("JM", 81.31);		//Jamaica
        ROBBERIES.put("KZ", 82.42);		//Kazakhstan
        ROBBERIES.put("SE", 86.20);		//Sweden
        ROBBERIES.put("BZ", 86.44);		//Belize
        ROBBERIES.put("GB", 87.52);		//United Kingdom (England and Wales)
        ROBBERIES.put("CY", 9.01);		//Cyprus
        ROBBERIES.put("MG", 9.18);		//Madagascar
        ROBBERIES.put("AD", 9.62);		//Andorra
        ROBBERIES.put("UG", 9.64);		//Uganda
        ROBBERIES.put("IT", 97.59);		//Italy
    }


}
