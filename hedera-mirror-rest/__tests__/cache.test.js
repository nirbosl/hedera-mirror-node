/*
 * Copyright (C) 2024 Hedera Hashgraph, LLC
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

import config from '../config';
import {Cache} from '../cache';
import {RedisContainer} from '@testcontainers/redis';
import {defaultBeforeAllTimeoutMillis} from './integrationUtils.js';

let cache;
let redisContainer;

beforeAll(async () => {
  config.redis.enabled = true;
  redisContainer = await new RedisContainer().withStartupTimeout(20000).start();
  logger.info('Started Redis container');
}, defaultBeforeAllTimeoutMillis);

afterAll(async () => {
  await redisContainer.stop({signal: 'SIGKILL', t: 5});
  logger.info('Stopped Redis container');
});

beforeEach(async () => {
  config.redis.uri = `0.0.0.0:${redisContainer.getMappedPort(6379)}`;
  cache = new Cache();
  await cache.clear();
});

afterEach(async () => {
  await cache.stop();
});

const loader = (keys) => keys.map((key) => `v${key}`);
const keyMapper = (key) => `k${key}`;

describe('get', () => {
  test('All keys from database', async () => {
    const values = await cache.get(['1', '2', '3'], loader, keyMapper);
    expect(values).toEqual(['v1', 'v2', 'v3']);
  });

  test('Some keys from database', async () => {
    const values = await cache.get(['1', '2', '3'], loader, keyMapper);
    expect(values).toEqual(['v1', 'v2', 'v3']);

    const newValues = await cache.get(['2', '3', '4'], loader, keyMapper);
    expect(newValues).toEqual(['v2', 'v3', 'v4']);
  });

  test('No keys from database', async () => {
    const values = await cache.get(['1', '2', '3'], loader, keyMapper);
    expect(values).toEqual(['v1', 'v2', 'v3']);

    const newValues = await cache.get(['1', '2', '3'], (k) => [], keyMapper);
    expect(newValues).toEqual(['v1', 'v2', 'v3']);
  });

  test('No keys provided', async () => {
    const values = await cache.get([], loader, keyMapper);
    expect(values).toEqual([]);
  });

  test('Disabled', async () => {
    config.redis.enabled = false;
    const values = await cache.get(['1', '2', '3'], loader, keyMapper);
    expect(values).toEqual(['v1', 'v2', 'v3']);
  });

  test('Unable to connect', async () => {
    config.redis.uri = 'redis://invalid:6379';
    cache = new Cache();
    const values = await cache.get(['1', '2', '3'], loader, keyMapper);
    expect(values).toEqual(['v1', 'v2', 'v3']);
  });
});
