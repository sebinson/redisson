/**
 * Copyright 2014 Nikita Koksharov, Nickolay Borbit
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.redisson;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;

import org.redisson.client.protocol.decoder.MapScanResult;

public class RedissonMapIterator<K, V, M> implements Iterator<M> {

    private Map<K, V> firstValues;
    private Iterator<Map.Entry<K, V>> iter;
    private long iterPos = 0;
    private InetSocketAddress client;

    private boolean removeExecuted;
    private Map.Entry<K, V> entry;

    private final RedissonMap<K, V> map;

    public RedissonMapIterator(RedissonMap<K, V> map) {
        this.map = map;
    }

    @Override
    public boolean hasNext() {
        if (iter == null || !iter.hasNext()) {
            MapScanResult<Object, V> res = map.scanIterator(client, iterPos);
            client = res.getRedisClient();
            if (iterPos == 0 && firstValues == null) {
                firstValues = (Map<K, V>) res.getMap();
            } else if (res.getMap().equals(firstValues)) {
                return false;
            }
            iter = ((Map<K, V>)res.getMap()).entrySet().iterator();
            iterPos = res.getPos();
        }
        return iter.hasNext();
    }

    @Override
    public M next() {
        if (!hasNext()) {
            throw new NoSuchElementException("No such element at index");
        }

        entry = iter.next();
        removeExecuted = false;
        return getValue(entry);
    }

    M getValue(Entry<K, V> entry) {
        return (M) entry;
    }

    @Override
    public void remove() {
        if (removeExecuted) {
            throw new IllegalStateException("Element been already deleted");
        }

        // lazy init iterator
        hasNext();
        iter.remove();
        map.fastRemove(entry.getKey());
        removeExecuted = true;
    }

}
