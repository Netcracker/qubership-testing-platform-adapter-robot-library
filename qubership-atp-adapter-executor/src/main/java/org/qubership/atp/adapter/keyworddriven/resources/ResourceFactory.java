/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.qubership.atp.adapter.keyworddriven.resources;

import org.qubership.atp.adapter.testcase.Config;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class ResourceFactory {
    public static final String CONFIG_KEY = "kdt.ResourceFactory";
    public static final String DEFAULT_FACTORY = "org.qubership.atp.adapter.keyworddriven.resources.DynamicResourceFactory";
    private static ResourceFactory instance;
    private static final Log log = LogFactory.getLog(ResourceFactory.class);
    private ConcurrentHashMap<Thread, LinkedList<Resource<?>>> resources = new ConcurrentHashMap();

    public ResourceFactory() {
    }

    public static ResourceFactory getInstance() throws RuntimeException {
        if (instance == null) {
            String factoryName = Config.getString("kdt.ResourceFactory", "org.qubership.atp.adapter.keyworddriven.resources.DynamicResourceFactory");

            Class factoryClazz;
            try {
                factoryClazz = Class.forName(factoryName);
            } catch (ClassNotFoundException var8) {
                throw new RuntimeException("Factory '" + factoryName + "' not found");
            } catch (ClassCastException var9) {
                throw new RuntimeException("Class " + factoryName + " is not a resource factory");
            }

            Class var2 = ResourceFactory.class;
            synchronized(ResourceFactory.class) {
                if (instance == null) {
                    try {
                        instance = (ResourceFactory)factoryClazz.newInstance();
                        if (log.isTraceEnabled()) {
                            log.trace("Resource factory initiated: " + instance);
                        }
                    } catch (InstantiationException var5) {
                        InstantiationException e = var5;
                        throw new RuntimeException("Factory " + factoryName + " instantiation failed", e);
                    } catch (IllegalAccessException var6) {
                        IllegalAccessException e = var6;
                        throw new RuntimeException("Factory " + factoryName + " instantiation failed", e);
                    }
                }
            }
        }

        return instance;
    }

    public <T> T get(Class<T> clazz) {
        Resource<T> initiatedResource = this.getInitiated(clazz);
        if (initiatedResource != null) {
            return initiatedResource.get();
        } else {
            Resource<T> res = this.getNewResource(clazz);
            if (res == null) {
                throw new RuntimeException("Resource with class " + clazz + " can not be initiated. Check your resource factory : " + this.getClass());
            } else {
                this.getResources().add(res);
                return res.get();
            }
        }
    }

    public <T> Resource<T> getInitiated(Class<T> clazz) {
        if (log.isTraceEnabled()) {
            log.trace("Trying to find resource " + clazz);
        }

        Iterator var2 = this.getResources().iterator();

        Resource res;
        do {
            if (!var2.hasNext()) {
                return null;
            }

            res = (Resource)var2.next();
        } while(!this.resourceIsInitiated(res, clazz));

        if (log.isTraceEnabled()) {
            log.trace("Already initiated resource found: " + res);
        }

        return res;
    }

    protected <T> boolean resourceIsInitiated(Resource<?> resource, Class<T> resourceType) {
        return resource.getParameterizedClass().isAssignableFrom(resourceType);
    }

    protected abstract <T> Resource<T> getNewResource(Class<T> var1);

    public void releaseResourcesForCurrentThread() {
        LinkedList<Resource<?>> resList = this.getResources();
        if (log.isTraceEnabled()) {
            log.trace("Start releasing thread resources: " + resList);
        }

        this.releaseResources(resList);
        this.resources.remove(Thread.currentThread());
    }

    public synchronized void releaseResourcesAll() {
        if (log.isTraceEnabled()) {
            log.trace("Start releasing all resources: " + this.resources);
        }

        Iterator var1 = this.resources.entrySet().iterator();

        while(var1.hasNext()) {
            Map.Entry<Thread, LinkedList<Resource<?>>> res = (Map.Entry)var1.next();
            this.releaseResources((Collection)res.getValue());
        }

        this.resources.clear();
    }

    public LinkedList<Resource<?>> getResources() {
        Thread t = Thread.currentThread();
        if (!this.resources.containsKey(t)) {
            this.resources.put(t, new LinkedList());
        }

        return (LinkedList)this.resources.get(t);
    }

    protected void releaseResources(Collection<Resource<?>> resCollection) {
        Resource res;
        for(Iterator var2 = resCollection.iterator(); var2.hasNext(); res.release()) {
            res = (Resource)var2.next();
            if (log.isTraceEnabled()) {
                log.trace("Resource releasing: " + res);
            }
        }

    }
}

