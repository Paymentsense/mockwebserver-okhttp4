/*
 *   Copyright (C) 2016 Red Hat, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *           http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.fabric8.mockwebserver.crud

import io.fabric8.mockwebserver.Context
import io.fabric8.mockwebserver.DefaultMockServer
import io.fabric8.mockwebserver.ServerRequest
import io.fabric8.mockwebserver.ServerResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody
import okhttp3.MediaType
import okhttp3.mockwebserver.MockWebServer
import spock.lang.Specification

class CrudDispatcherTest extends Specification {

    AttributeExtractor extractor = new AttributeExtractor() {

        @Override
        AttributeSet extract(String path) {
            AttributeSet set = new AttributeSet()

            String[] parts = path.split("/")
            if (parts.length > 2) {
                set = set.add(new Attribute("namespace", parts[2]))
            }

            if (parts.length > 4) {
                set = set.add(new Attribute("name", parts[4]))
            }
            return set
        }

        @Override
        AttributeSet extract(Object object) {
            return null
        }
    }

    ResponseComposer composer = new ResponseComposer() {
        @Override
        String compose(Collection<String> items) {
            StringBuilder sb = new StringBuilder();
            for (String item : items) {
                sb.append(item).append(" ")
            }
            return sb.toString().trim()
        }
    }

    def "should be able to get after a post"() {
        given:
        DefaultMockServer server = new DefaultMockServer(new Context(), new  MockWebServer(), new HashMap<ServerRequest, Queue<ServerResponse>>(), new CrudDispatcher(extractor, composer), false)
        when:
        server.start()
        then:
        OkHttpClient client = new OkHttpClient()
        Request post = new Request.Builder().post(RequestBody.create(MediaType.parse("text/html"), "one")).url(server.url("/namespace/test/name/one")).build()
        client.newCall(post).execute()
        Request get = new Request.Builder().get().url(server.url("/namespace/test/name/one")).build()
        Response response = client.newCall(get).execute()
        assert response.body().string().equals("one")
    }

    def "should be able to delete after a post"() {
        given:
        DefaultMockServer server = new DefaultMockServer(new Context(), new MockWebServer(), new HashMap<ServerRequest, Queue<ServerResponse>>(), new CrudDispatcher(extractor, composer), false)
        when:
        server.start()
        then:
        OkHttpClient client = new OkHttpClient()
        Request post = new Request.Builder().post(RequestBody.create(MediaType.parse("text/html"), "one")).url(server.url("/namespace/test/name/one")).build()
        client.newCall(post).execute()
        Request get = new Request.Builder().delete().url(server.url("/namespace/test/name/one")).build()
        Response response = client.newCall(get).execute()
        assert response.successful

        Request getMissing = new Request.Builder().delete().url(server.url("/namespace/test/name/two")).build()
        Response responseMissing = client.newCall(getMissing).execute()
    assert !responseMissing.successful
    }

}