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

import android.app.Activity;
import android.util.Pair;

import androidx.fragment.app.Fragment;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.internal.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.internal.ui.AuthorizationAgent;
import com.microsoft.identity.common.internal.ui.browser.BrowserDescriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class InteractiveTokenCommandParameters extends TokenCommandParameters {

    @EqualsAndHashCode.Exclude
    private transient Activity activity;

    @EqualsAndHashCode.Exclude
    private transient Fragment fragment;

    private transient List<BrowserDescriptor> browserSafeList;

    private transient HashMap<String, String> requestHeaders;

    private boolean brokerBrowserSupportEnabled;

    private String loginHint;

    @Expose()
    private OpenIdConnectPromptParameter prompt;

    @Expose()
    private AuthorizationAgent authorizationAgent;

    @Expose()
    private boolean isWebViewZoomEnabled;

    @Expose()
    private boolean isWebViewZoomControlsEnabled;

    private List<Pair<String, String>> extraQueryStringParameters;

    private List<String> extraScopesToConsent;

    public List<Pair<String, String>> getExtraQueryStringParameters() {
        return this.extraQueryStringParameters == null ? null : new ArrayList<>(this.extraQueryStringParameters);
    }

    public List<String> getExtraScopesToConsent() {
        return this.extraScopesToConsent == null ? null : new ArrayList<>(this.extraScopesToConsent);
    }

    public List<BrowserDescriptor> getBrowserSafeList() {
        return this.browserSafeList == null ? null : new ArrayList<>(this.browserSafeList);
    }
}
