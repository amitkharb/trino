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
package io.trino.plugin.prometheus;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.net.HttpHeaders;
import com.google.inject.ConfigurationException;
import com.google.inject.spi.Message;
import io.airlift.configuration.Config;
import io.airlift.configuration.ConfigDescription;
import io.airlift.configuration.ConfigSecuritySensitive;
import io.airlift.units.Duration;
import io.airlift.units.MinDuration;
import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotNull;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.lang.String.format;
import static java.util.Locale.ENGLISH;

public class PrometheusConnectorConfig
{
    private URI prometheusURI = URI.create("http://localhost:9090");
    private Duration queryChunkSizeDuration = new Duration(1, TimeUnit.DAYS);
    private Duration maxQueryRangeDuration = new Duration(21, TimeUnit.DAYS);
    private Duration cacheDuration = new Duration(30, TimeUnit.SECONDS);
    private Duration readTimeout = new Duration(10, TimeUnit.SECONDS);
    private String httpAuthHeaderName = HttpHeaders.AUTHORIZATION;
    private File bearerTokenFile;
    private String user;
    private String password;
    private boolean caseInsensitiveNameMatching;
    private Map<String, String> additionalHeaders = ImmutableMap.of();
    private String matchString;
    private Set<String> queryFunctions = ImmutableSet.of();

    @NotNull
    public URI getPrometheusURI()
    {
        return prometheusURI;
    }

    @Config("prometheus.uri")
    @ConfigDescription("Where to find Prometheus coordinator host")
    public PrometheusConnectorConfig setPrometheusURI(URI prometheusURI)
    {
        this.prometheusURI = prometheusURI;
        return this;
    }

    @MinDuration("1ms")
    public Duration getQueryChunkSizeDuration()
    {
        return queryChunkSizeDuration;
    }

    @Config("prometheus.query.chunk.size.duration")
    @ConfigDescription("The duration of each query to Prometheus")
    public PrometheusConnectorConfig setQueryChunkSizeDuration(Duration queryChunkSizeDuration)
    {
        this.queryChunkSizeDuration = queryChunkSizeDuration;
        return this;
    }

    @MinDuration("1ms")
    public Duration getMaxQueryRangeDuration()
    {
        return maxQueryRangeDuration;
    }

    @Config("prometheus.max.query.range.duration")
    @ConfigDescription("Width of overall query to Prometheus, will be divided into prometheus.query.chunk.size.duration queries")
    public PrometheusConnectorConfig setMaxQueryRangeDuration(Duration maxQueryRangeDuration)
    {
        this.maxQueryRangeDuration = maxQueryRangeDuration;
        return this;
    }

    @MinDuration("1s")
    public Duration getCacheDuration()
    {
        return cacheDuration;
    }

    @Config("prometheus.cache.ttl")
    @ConfigDescription("How long values from this config file are cached")
    public PrometheusConnectorConfig setCacheDuration(Duration cacheConfigDuration)
    {
        this.cacheDuration = cacheConfigDuration;
        return this;
    }

    public String getHttpAuthHeaderName()
    {
        return httpAuthHeaderName;
    }

    @Config("prometheus.auth.http.header.name")
    @ConfigDescription("Name of the HTTP header to use for authorization")
    public PrometheusConnectorConfig setHttpAuthHeaderName(String httpHeaderName)
    {
        this.httpAuthHeaderName = httpHeaderName;
        return this;
    }

    public Optional<File> getBearerTokenFile()
    {
        return Optional.ofNullable(bearerTokenFile);
    }

    @Config("prometheus.bearer.token.file")
    @ConfigDescription("File holding bearer token if needed for access to Prometheus")
    public PrometheusConnectorConfig setBearerTokenFile(File bearerTokenFile)
    {
        this.bearerTokenFile = bearerTokenFile;
        return this;
    }

    @NotNull
    public Optional<String> getUser()
    {
        return Optional.ofNullable(user);
    }

    @Config("prometheus.auth.user")
    public PrometheusConnectorConfig setUser(String user)
    {
        this.user = user;
        return this;
    }

    @NotNull
    public Optional<String> getPassword()
    {
        return Optional.ofNullable(password);
    }

    @Config("prometheus.auth.password")
    @ConfigSecuritySensitive
    public PrometheusConnectorConfig setPassword(String password)
    {
        this.password = password;
        return this;
    }

    @MinDuration("1s")
    public Duration getReadTimeout()
    {
        return readTimeout;
    }

    @Config("prometheus.read-timeout")
    @ConfigDescription("How much time a query to Prometheus has before timing out")
    public PrometheusConnectorConfig setReadTimeout(Duration readTimeout)
    {
        this.readTimeout = readTimeout;
        return this;
    }

    public boolean isCaseInsensitiveNameMatching()
    {
        return caseInsensitiveNameMatching;
    }

    @Config("prometheus.case-insensitive-name-matching")
    @ConfigDescription("Where to match the prometheus metric name case insensitively ")
    public PrometheusConnectorConfig setCaseInsensitiveNameMatching(boolean caseInsensitiveNameMatching)
    {
        this.caseInsensitiveNameMatching = caseInsensitiveNameMatching;
        return this;
    }

    public Map<String, String> getAdditionalHeaders()
    {
        return additionalHeaders;
    }

    @Config("prometheus.http.additional-headers")
    @ConfigDescription("Comma separated key:value pairs to be sent with the HTTP request to Prometheus as additional headers")
    public PrometheusConnectorConfig setAdditionalHeaders(String httpHeaders)
    {
        try {
            // we allow escaping the delimiters like , and : using back-slash.
            // To support that we create a negative lookbehind of , and : which
            // are not preceded by a back-slash.
            String headersDelim = "(?<!\\\\),";
            String kvDelim = "(?<!\\\\):";
            Map<String, String> temp = new HashMap<>();
            if (httpHeaders != null) {
                for (String kv : httpHeaders.split(headersDelim)) {
                    String key = kv.split(kvDelim, 2)[0].trim();
                    String val = kv.split(kvDelim, 2)[1].trim();
                    temp.put(key, val);
                }
                this.additionalHeaders = ImmutableMap.copyOf(temp);
            }
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(format("Invalid format for 'prometheus.http.additional-headers' because %s. Value provided is %s", e.getMessage(), httpHeaders), e);
        }
        return this;
    }

    public Optional<String> getMatchString()
    {
        return Optional.ofNullable(matchString);
    }

    @Config("prometheus.query.match.string")
    @ConfigDescription("match[] filter to be used in Prometheus HTTP API")
    public PrometheusConnectorConfig setMatchString(String matchString)
    {
        this.matchString = matchString;
        return this;
    }

    public Set<String> getQueryFunctions()
    {
        return queryFunctions;
    }

    @Config("prometheus.query.functions")
    @ConfigDescription("Comma separated list of functions to be sent to Prometheus HTTP API as part of query")
    public PrometheusConnectorConfig setQueryFunctions(List<String> queryFunctions)
    {
        this.queryFunctions = queryFunctions.stream()
            .map(value -> value.toLowerCase(ENGLISH))
            .collect(toImmutableSet());
        return this;
    }

    @PostConstruct
    public void checkConfig()
    {
        long maxQueryRangeDuration = (long) getMaxQueryRangeDuration().getValue(TimeUnit.SECONDS);
        long queryChunkSizeDuration = (long) getQueryChunkSizeDuration().getValue(TimeUnit.SECONDS);
        if (maxQueryRangeDuration < queryChunkSizeDuration) {
            throw new ConfigurationException(ImmutableList.of(new Message("prometheus.max.query.range.duration must be greater than prometheus.query.chunk.size.duration")));
        }
        if (getBearerTokenFile().isPresent() && (getUser().isPresent() || getPassword().isPresent())) {
            throw new IllegalStateException("Either on of bearer token file or basic authentication should be used");
        }
        if (getUser().isPresent() ^ getPassword().isPresent()) {
            throw new IllegalStateException("Both username and password must be set when using basic authentication");
        }
        if (getAdditionalHeaders().containsKey(httpAuthHeaderName)) {
            throw new IllegalStateException("Additional headers can not include: " + httpAuthHeaderName);
        }
    }
}
