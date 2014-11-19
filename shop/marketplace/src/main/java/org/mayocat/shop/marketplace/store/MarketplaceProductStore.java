/*
 * Copyright (c) 2012, Mayocat <hello@mayocat.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mayocat.shop.marketplace.store;

import java.util.List;

import org.mayocat.accounts.model.Tenant;
import org.mayocat.shop.catalog.model.Product;
import org.mayocat.shop.marketplace.model.EntityAndTenant;
import org.xwiki.component.annotation.Role;

/**
 * @version $Id$
 */
@Role
public interface MarketplaceProductStore
{
    Product findBySlugAndTenant(String slug, String tenantSlug);

    List<Product> findAllNotVariants(Integer number, Integer offset);

    Integer countAllNotVariants();

    List<Product> findAllWithTitleLike(String title, Integer number, Integer offset);

    Integer countAllWithTitleLike(String title);

    List<Product> findAllForTenant(Tenant tenant, Integer number, Integer offset);
}
