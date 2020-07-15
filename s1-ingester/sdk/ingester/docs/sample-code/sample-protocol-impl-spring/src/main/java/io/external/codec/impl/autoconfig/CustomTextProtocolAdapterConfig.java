/*
 * =========================================================================
 * Copyright (c) 2013-2014 Pivotal Software, Inc. All Rights Reserved.
 * This product is protected by U.S. and international copyright
 * and intellectual property laws. Pivotal products are covered by
 * one or more patents listed at http://www.pivotal.io/patents.
 * =========================================================================
 */
package io.external.codec.impl.autoconfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.external.codec.impl.CustomTextProtocolAdapter;

@Configuration
public class CustomTextProtocolAdapterConfig {

    @Bean(name = { "customProtocolAdapter" })
    public CustomTextProtocolAdapter createCustomProtocolAdapter() {
        return new CustomTextProtocolAdapter();
    }

}
