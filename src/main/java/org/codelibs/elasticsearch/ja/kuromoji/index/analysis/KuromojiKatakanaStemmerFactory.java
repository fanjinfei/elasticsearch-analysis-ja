/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.codelibs.elasticsearch.ja.kuromoji.index.analysis;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ja.JapaneseKatakanaStemFilter;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.env.Environment;
import org.elasticsearch.index.IndexSettings;
import org.elasticsearch.index.analysis.AbstractTokenFilterFactory;

public class KuromojiKatakanaStemmerFactory extends AbstractTokenFilterFactory {

    private final int minimumLength;

    public KuromojiKatakanaStemmerFactory(final IndexSettings indexSettings, final Environment environment, final String name, final Settings settings) {
        super(indexSettings, name, settings);
        minimumLength = settings.getAsInt("minimum_length", JapaneseKatakanaStemFilter.DEFAULT_MINIMUM_LENGTH);
    }

    @Override
    public TokenStream create(final TokenStream tokenStream) {
        return new JapaneseKatakanaStemFilter(tokenStream, minimumLength);
    }
}
