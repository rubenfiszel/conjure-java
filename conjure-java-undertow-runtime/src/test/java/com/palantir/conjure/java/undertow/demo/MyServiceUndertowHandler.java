/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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

package com.palantir.conjure.java.undertow.demo;

import com.google.common.reflect.TypeToken;
import com.palantir.conjure.java.undertow.lib.Routable;
import com.palantir.conjure.java.undertow.lib.RoutingRegistry;
import com.palantir.conjure.java.undertow.lib.Serializer;
import com.palantir.conjure.java.undertow.lib.internal.StringDeserializers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.PathTemplateMatch;
import io.undertow.util.StatusCodes;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Handler as generated by conjure-undertow. */
public final class MyServiceUndertowHandler implements Routable {

    private final MyService delegate;
    private final Serializer bodySerializer;

    public MyServiceUndertowHandler(
            MyService delegate,
            Serializer bodySerializer) {
        this.delegate = delegate;
        this.bodySerializer = bodySerializer;
    }

    @Override
    public void register(RoutingRegistry routingRegistry) {
        routingRegistry.get("/inc/{base}/{numHours}", "", "", new IncrementTimeHandler())
                .post("/issunday", "", "", new IsSundayHandler())
                .get("/traceId", "", "", new TraceIdHandler())
                .get("/maybeString/{shouldReturnString}", "", "", new MaybeStringHandler());
    }

    // TODO(nmiyake): If we decide to go generated handler route, rewrite all of this example code
    private class IncrementTimeHandler implements HttpHandler {

        @Override
        public void handleRequest(HttpServerExchange exchange) throws IOException {
            Map<String, String> params = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY).getParameters();
            OffsetDateTime base = StringDeserializers.deserializeDateTime(params.get("base"));
            int numHours = StringDeserializers.deserializeInteger(params.get("numHours"));

            OffsetDateTime result = delegate.incrementTime(base, numHours);
            bodySerializer.serialize(result, exchange.getOutputStream());
        }
    }

    private class IsSundayHandler implements HttpHandler {

        private final TypeToken<List<OffsetDateTime>> datesType = new TypeToken<List<OffsetDateTime>>() {};

        @Override
        public void handleRequest(HttpServerExchange exchange) throws IOException {
            List<OffsetDateTime> dates = bodySerializer.deserialize(exchange.getInputStream(), datesType);

            Map<OffsetDateTime, Boolean> result = delegate.isSunday(dates);
            bodySerializer.serialize(result, exchange.getOutputStream());
        }
    }

    private class TraceIdHandler implements HttpHandler {

        @Override
        public void handleRequest(HttpServerExchange exchange) throws IOException {
            String result = delegate.returnTrace();
            bodySerializer.serialize(result, exchange.getOutputStream());
        }
    }

    private class MaybeStringHandler implements HttpHandler {

        // private final TypeToken<Boolean> shouldReturnStringType = new TypeToken<Boolean>() {};

        @Override
        public void handleRequest(HttpServerExchange exchange) throws IOException {
            Map<String, String> params = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY).getParameters();
            // boolean shouldReturnString =
            //         plainSerializer.deserialize(params.get("shouldReturnString"), shouldReturnStringType);
            boolean shouldReturnString = StringDeserializers.deserializeBoolean(params.get("shouldReturnString"));
            Optional<String> result = delegate.maybeString(shouldReturnString);
            if (result.isPresent()) {
                bodySerializer.serialize(result, exchange.getOutputStream());
            } else {
                exchange.setStatusCode(StatusCodes.NO_CONTENT);
            }
        }
    }
}
