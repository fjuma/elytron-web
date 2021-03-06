/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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
package org.wildfly.elytron.web.undertow.server;

import static org.wildfly.common.Assert.checkNotNullParam;

import java.util.concurrent.Callable;

import org.wildfly.security.auth.server.SecurityIdentity;

import io.undertow.security.api.SecurityContext;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * A {@link HttpHandler} to be placed after the request has switched to blocking mode to associate the {@link SecurityIdentity}
 * with the current thread.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ElytronRunAsHandler implements HttpHandler {

    private final HttpHandler next;

    public ElytronRunAsHandler(HttpHandler next) {
        this.next = checkNotNullParam("next", next);
    }

    /**
     * @see io.undertow.server.HttpHandler#handleRequest(io.undertow.server.HttpServerExchange)
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        SecurityContext securityContext = exchange.getSecurityContext();
        Account account = securityContext != null ? securityContext.getAuthenticatedAccount() : null;
        SecurityIdentity securityIdentity = (account instanceof ElytronAccount) ? ((ElytronAccount)account).getSecurityIdentity() : null;

        if (securityIdentity != null) {
            securityIdentity.runAs((Callable<Void>) () -> {
                next.handleRequest(exchange);
                return null;
            });
        } else {
            next.handleRequest(exchange);
        }
    }

}
