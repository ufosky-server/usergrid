/*
 * Copyright 2014 The Apache Software Foundation.
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
package org.apache.usergrid.corepersistence;

import org.apache.usergrid.persistence.collection.CollectionScope;
import org.apache.usergrid.persistence.collection.EntityCollectionManager;
import org.apache.usergrid.persistence.collection.EntityCollectionManagerFactory;
import org.apache.usergrid.persistence.core.scope.OrganizationScope;
import org.apache.usergrid.persistence.graph.GraphManager;
import org.apache.usergrid.persistence.graph.GraphManagerFactory;
import org.apache.usergrid.persistence.index.EntityIndex;
import org.apache.usergrid.persistence.index.EntityIndexFactory;
import org.apache.usergrid.utils.LRUCache2;

class CpManagerCache {

    private final EntityCollectionManagerFactory ecmf;
    private final EntityIndexFactory eif;
    private final GraphManagerFactory gmf;

    // TODO: consider making these cache sizes and timeouts configurable
    private final LRUCache2<CollectionScope, EntityCollectionManager> ecmCache
            = new LRUCache2<CollectionScope, EntityCollectionManager>(50, 1 * 60 * 60 * 1000);

    private final LRUCache2<FullScopeHashKey, EntityIndex> eiCache
            = new LRUCache2<FullScopeHashKey, EntityIndex>(50, 1 * 60 * 60 * 1000);

    private final LRUCache2<OrganizationScope, GraphManager> gmCache
            = new LRUCache2<OrganizationScope, GraphManager>(50, 1 * 60 * 60 * 1000);

    public CpManagerCache(
            EntityCollectionManagerFactory ecmf, EntityIndexFactory eif, GraphManagerFactory gmf) {
        this.ecmf = ecmf;
        this.eif = eif;
        this.gmf = gmf;

    }

    public EntityCollectionManager getEntityCollectionManager(CollectionScope scope) {

        EntityCollectionManager ecm = ecmCache.get(scope);

        if (ecm == null) {
            ecm = ecmf.createCollectionManager(scope);
            ecmCache.put(scope, ecm);
        }
        return ecm;
    }

    public EntityIndex getEntityIndex(OrganizationScope orgScope, CollectionScope collScope) {

        FullScopeHashKey fshk = new FullScopeHashKey(orgScope, collScope);
        EntityIndex ei = eiCache.get(fshk);

        if (ei == null) {
            ei = eif.createEntityIndex(orgScope, collScope);
            eiCache.put(fshk, ei);
        }
        return ei;
    }

    public GraphManager getGraphManager(OrganizationScope scope) {

        GraphManager gm = gmCache.get(scope);

        if (gm == null) {
            gm = gmf.createEdgeManager(scope);
            gmCache.put(scope, gm);
        }
        return gm;
    }

    public static class FullScopeHashKey {

        private OrganizationScope orgScope;
        private CollectionScope collScope;

        public FullScopeHashKey(OrganizationScope orgScope, CollectionScope collScope) {
            this.orgScope = orgScope;
            this.collScope = collScope;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof FullScopeHashKey)) {
                return false;
            }
            FullScopeHashKey other = (FullScopeHashKey) o;
            if (!other.collScope.equals(this.collScope)) {
                return false;
            }
            if (!other.orgScope.equals(this.orgScope)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = 31 * orgScope.hashCode();
            result = 31 * result + collScope.hashCode();
            return result;
        }
    }
}