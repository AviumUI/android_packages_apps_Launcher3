/*
 * Copyright (C) 2026 The AviumUI Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.avium.launcher.data;

import com.android.launcher3.model.data.AppInfo;
import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

public class AppCategorizer {

    public static class CategorizedApps {
        public final TreeMap<String, List<AppInfo>> appsMap;
        public final List<String> letters;

        public CategorizedApps(TreeMap<String, List<AppInfo>> appsMap, List<String> letters) {
            this.appsMap = appsMap;
            this.letters = letters;
        }
    }

    public CategorizedApps categorize(AppInfo[] apps) {
        TreeMap<String, List<AppInfo>> appsMap = new TreeMap<>(new LetterComparator());

        for (AppInfo app : apps) {
            String category = getLetterCategory(app.title.toString());
            List<AppInfo> list = appsMap.computeIfAbsent(category, k -> new ArrayList<>());
            list.add(app);
        }

        for (List<AppInfo> list : appsMap.values()) {
            list.sort(Comparator.comparing(a -> a.title.toString().toLowerCase(Locale.getDefault())));
        }

        List<String> letters = new ArrayList<>(appsMap.keySet());
        return new CategorizedApps(appsMap, letters);
    }

    private String getLetterCategory(String title) {
        if (title == null || title.isEmpty()) return "#";
        char c = title.charAt(0);
        if (c >= 'A' && c <= 'Z') return String.valueOf(c);
        if (c >= 'a' && c <= 'z') return String.valueOf(Character.toUpperCase(c));
        if (c >= '0' && c <= '9') return "#";
        if (Pinyin.isChinese(c)) {
            String pinyin = Pinyin.toPinyin(c);
            if (pinyin != null && !pinyin.isEmpty()) {
                char pinyinFirstChar = pinyin.charAt(0);
                if (pinyinFirstChar >= 'A' && pinyinFirstChar <= 'Z') {
                    return String.valueOf(pinyinFirstChar);
                }
            }
        }
        return "#";
    }

    private static class LetterComparator implements Comparator<String> {
        @Override
        public int compare(String s1, String s2) {
            boolean isS1Letter = s1.length() == 1 && s1.charAt(0) >= 'A' && s1.charAt(0) <= 'Z';
            boolean isS2Letter = s2.length() == 1 && s2.charAt(0) >= 'A' && s2.charAt(0) <= 'Z';

            if (isS1Letter && isS2Letter) {
                return s1.compareTo(s2);
            } else if (isS1Letter) {
                return -1;
            } else if (isS2Letter) {
                return 1;
            } else if (s1.equals("#") && !s2.equals("#")) {
                return 1;
            } else if (!s1.equals("#") && s2.equals("#")) {
                return -1;
            } else {
                return s1.compareTo(s2);
            }
        }
    }
}
