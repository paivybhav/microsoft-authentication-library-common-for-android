// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.commands.parameters;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authscheme.AbstractAuthenticationScheme;
import com.microsoft.identity.common.internal.dto.IAccountRecord;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class TokenCommandParameters extends CommandParameters {

    private static final String TAG = TokenCommandParameters.class.getSimpleName();

    private IAccountRecord account;

    @Expose()
    private Set<String> scopes;

    @Expose()
    private Authority authority;

    @Expose()
    private String claimsRequestJson;

    @Expose()
    private AbstractAuthenticationScheme authenticationScheme;

    @Expose()
    private boolean forceRefresh;

    public Set<String> getScopes() {
        return this.scopes == null ? null : new HashSet<>(this.scopes);
    }

    public void validate() throws ArgumentException {
        final String methodName = ":validate";

        Logger.verbose(
                TAG + methodName,
                "Validating operation params..."
        );

        boolean validScopeArgument = false;

        if (scopes != null) {
            scopes.removeAll(Arrays.asList("", null));
            if (scopes.size() > 0) {
                validScopeArgument = true;
            }
        }

        if (!validScopeArgument) {
            if (this instanceof SilentTokenCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null"
                );
            }
            if (this instanceof InteractiveTokenCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        ArgumentException.SCOPE_ARGUMENT_NAME,
                        "scope is empty or null");
            }
        }

        // AuthenticationScheme is present...
        if (null == authenticationScheme) {
            if (this instanceof SilentTokenCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME,
                        ArgumentException.AUTHENTICATION_SCHEME_ARGUMENT_NAME,
                        "authentication scheme is undefined"
                );
            }

            if (this instanceof InteractiveTokenCommandParameters) {
                throw new ArgumentException(
                        ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME,
                        ArgumentException.AUTHENTICATION_SCHEME_ARGUMENT_NAME,
                        "authentication scheme is undefined"
                );
            }
        }
    }
}
