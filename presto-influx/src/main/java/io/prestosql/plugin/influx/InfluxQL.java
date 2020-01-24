/*
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

package io.prestosql.plugin.influx;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableSet;
import io.airlift.slice.Slice;

import java.util.Locale;
import java.util.Set;

/* A query builder that takes care correctly quoting identifiers and string values */

public class InfluxQL
{
    private static final Set<String> RESERVED_WORDS = ImmutableSet.copyOf(
            ("all alter any as asc begin by create continuous " +
            "database databases default delete desc destinations " +
            "diagnostics distinct drop duration end every explain " +
            "field for from grant grants group groups in inf " +
            "insert into key keys kill limit show measurement " +
            "measurements name offset on order password policy " +
            "policies privileges queries query read replication resample " +
            "retention revoke select series set shard shards slimit " +
            "soffset stats subscription subscriptions tag to user" +
            "users values where with write").split(" "));
    private final StringBuilder influxQL;

    public InfluxQL()
    {
        influxQL = new StringBuilder();
    }

    @JsonCreator
    public InfluxQL(@JsonProperty("q") String prefix)
    {
        influxQL = new StringBuilder(prefix);
    }

    public InfluxQL append(InfluxQL fragment)
    {
        influxQL.append(fragment);
        return this;
    }

    public InfluxQL append(String s)
    {
        influxQL.append(s);
        return this;
    }

    public InfluxQL append(char ch)
    {
        influxQL.append(ch);
        return this;
    }

    public InfluxQL append(long l)
    {
        influxQL.append(l);
        return this;
    }

    public InfluxQL append(int i)
    {
        influxQL.append(i);
        return this;
    }

    public int getPos()
    {
        return influxQL.length();
    }

    public void truncate(int pos)
    {
        InfluxError.GENERAL.check(influxQL.length() >= pos, "bad truncation (" + pos + " > " + influxQL.length() + ")", influxQL);
        influxQL.setLength(pos);
    }

    public InfluxQL add(InfluxColumn column)
    {
        addIdentifier(column.getInfluxName());
        return this;
    }

    public InfluxQL add(Object value)
    {
        InfluxError.GENERAL.check(!(value instanceof InfluxColumn), "value cannot be a column", value);
        if (value == null) {
            influxQL.append("null");
        }
        else if (value instanceof Slice) {
            quote(((Slice) value).toStringUtf8(), '\'');
        }
        else if (value instanceof Number || value instanceof Boolean) {
            influxQL.append(value);
        }
        else {
            quote(value.toString(), '\'');
        }
        return this;
    }

    public InfluxQL addIdentifier(String identifier)
    {
        boolean safe = true;
        for (int i = 0; i < identifier.length() && safe; i++) {
            char ch = identifier.charAt(i);
            safe = (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') || (i > 0 && ch >= '0' && ch <= '9') || ch == '_';
        }
        if (safe) {
            safe = !RESERVED_WORDS.contains(identifier.toLowerCase(Locale.ENGLISH));
        }
        if (safe) {
            influxQL.append(identifier);
        }
        else {
            quote(identifier, '"');
        }
        return this;
    }

    public void quote(String value, char delimiter)
    {
        append(delimiter);
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            InfluxError.BAD_VALUE.check(ch >= ' ', "illegal value", value);
            if (ch == '\\' || ch == delimiter) {
                append('\\');
            }
            append(ch);
        }
        append(delimiter);
    }

    public boolean isEmpty()
    {
        return influxQL.length() == 0;
    }

    @JsonProperty("q")
    @Override
    public String toString()
    {
        return influxQL.toString();
    }
}