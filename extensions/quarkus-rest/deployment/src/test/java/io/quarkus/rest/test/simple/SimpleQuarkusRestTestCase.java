package io.quarkus.rest.test.simple;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.function.Supplier;

import org.hamcrest.Matchers;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;
import io.restassured.http.Headers;

public class SimpleQuarkusRestTestCase {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(SimpleQuarkusRestResource.class, Person.class,
                                    TestRequestFilter.class, TestRequestFilterWithHighPriority.class,
                                    TestRequestFilterWithHighestPriority.class, ResourceInfoInjectingFilter.class,
                                    Foo.class, Bar.class,
                                    TestFooRequestFilter.class, TestBarRequestFilter.class, TestFooBarRequestFilter.class,
                                    TestFooResponseFilter.class, TestBarResponseFilter.class, TestFooBarResponseFilter.class,
                                    TestResponseFilter.class, HelloService.class, TestException.class,
                                    TestExceptionMapper.class, TestPreMatchRequestFilter.class,
                                    FeatureMappedException.class, FeatureMappedExceptionMapper.class,
                                    FeatureRequestFilterWithNormalPriority.class, FeatureRequestFilterWithHighestPriority.class,
                                    FeatureResponseFilter.class, DynamicFeatureRequestFilterWithLowPriority.class,
                                    TestFeature.class, TestDynamicFeature.class,
                                    SubResource.class, RootAResource.class, RootBResource.class,
                                    TestWriter.class, TestClass.class);
                }
            });

    @Test
    public void simpleTest() {
        RestAssured.get("/simple")
                .then().body(Matchers.equalTo("GET"));
        RestAssured.get("/simple/foo")
                .then().body(Matchers.equalTo("GET:foo"));

        RestAssured.post("/simple")
                .then().body(Matchers.equalTo("POST"));

        RestAssured.get("/missing")
                .then().statusCode(404);

        RestAssured.post("/missing")
                .then().statusCode(404);

        RestAssured.delete("/missing")
                .then().statusCode(404);

        RestAssured.delete("/simple")
                .then().body(Matchers.equalTo("DELETE"));

        RestAssured.put("/simple")
                .then().body(Matchers.equalTo("PUT"));

        RestAssured.head("/simple")
                .then().header("Stef", "head");

        RestAssured.options("/simple")
                .then().body(Matchers.equalTo("OPTIONS"));

        RestAssured.patch("/simple")
                .then().body(Matchers.equalTo("PATCH"));
    }

    @Test
    public void testInjection() {
        RestAssured.get("/simple/hello")
                .then().body(Matchers.equalTo("Hello"));
    }

    @Test
    public void testSubResource() {
        RestAssured.get("/simple/sub/otherSub")
                .then().body(Matchers.equalTo("otherSub"));
        RestAssured.get("/simple/sub")
                .then().body(Matchers.equalTo("sub"));
    }

    @Test
    public void testParams() {
        RestAssured.with()
                .queryParam("q", "qv")
                .header("h", "123")
                .formParam("f", "fv")
                .post("/simple/params/pv")
                .then().body(Matchers.equalTo("params: p: pv, q: qv, h: 123, f: fv"));
    }

    @Test
    public void testJson() {
        RestAssured.get("/simple/person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        RestAssured.with().body(person).post("/simple/person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));
    }

    @Test
    public void testLargeJsonPost() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; ++i) {
            sb.append("abc");
        }
        String longString = sb.toString();
        Person person = new Person();
        person.setFirst(longString);
        person.setLast(longString);
        RestAssured.with().body(person).post("/simple/person-large")
                .then().body("first", Matchers.equalTo(longString)).body("last", Matchers.equalTo(longString));
    }

    @Test
    public void testAsyncJson() {
        RestAssured.get("/simple/async-person")
                .then().body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));
    }

    @Test
    public void testValidatedJson() {
        Person person = new Person();
        person.setFirst("Bob");
        person.setLast("Builder");
        RestAssured.with().body(person).post("/simple/person-validated")
                .then().statusCode(200).body("first", Matchers.equalTo("Bob")).body("last", Matchers.equalTo("Builder"));

        RestAssured.with().body(person).post("/simple/person-invalid-result")
                .then()
                .statusCode(500)
                .contentType("application/json");

        person.setLast(null);
        RestAssured.with().body(person).post("/simple/person-validated")
                .then()
                .statusCode(400)
                .contentType("application/json");
    }

    @Test
    public void testBlocking() {
        RestAssured.get("/simple/blocking")
                .then().body(Matchers.equalTo("true"));
    }

    @Test
    public void testPreMatchFilter() {
        RestAssured.get("/simple/pre-match")
                .then().body(Matchers.equalTo("pre-match-post"));
        RestAssured.post("/simple/pre-match")
                .then().body(Matchers.equalTo("pre-match-post"));
    }

    @Test
    public void testFilters() {
        Headers headers = RestAssured.get("/simple/filters")
                .then().extract().headers();
        assertThat(headers.getValues("filter-request")).containsOnly("authentication-authorization-default");
        assertThat(headers.getValues("filter-response")).containsOnly("default");

        headers = RestAssured.get("/simple/fooFilters")
                .then().extract().headers();
        assertThat(headers.getValues("filter-request")).containsOnly("authentication-authorization-foo-default");
        assertThat(headers.getValues("filter-response")).containsOnly("default-foo");

        headers = RestAssured.get("/simple/barFilters")
                .then().extract().headers();
        assertThat(headers.getValues("filter-request")).containsOnly("authentication-authorization-default-bar");
        assertThat(headers.getValues("filter-response")).containsOnly("default-bar");

        headers = RestAssured.get("/simple/fooBarFilters")
                .then().extract().headers();
        assertThat(headers.getValues("filter-request")).containsOnly("authentication-authorization-foo-default-bar-foobar");
        assertThat(headers.getValues("filter-response")).containsOnly("default-foo-bar-foobar");
    }

    @Test
    public void testProviders() {
        RestAssured.get("/simple/providers")
                .then().body(Matchers.containsString("TestException"))
                .statusCode(200);
    }

    @Test
    public void testException() {
        RestAssured.get("/simple/mapped-exception")
                .then().body(Matchers.equalTo("OK"))
                .statusCode(666);
        RestAssured.get("/simple/unknown-exception")
                .then().statusCode(500);
        RestAssured.get("/simple/web-application-exception")
                .then().body(Matchers.equalTo("OK"))
                .statusCode(666);
    }

    @Test
    public void testWriter() {
        RestAssured.get("/simple/lookup-writer")
                .then().body(Matchers.equalTo("OK"));
        RestAssured.get("/simple/writer")
                .then().body(Matchers.equalTo("WRITER"));

        RestAssured.get("/simple/fast-writer")
                .then().body(Matchers.equalTo("OK"));

        RestAssured.get("/simple/writer/vertx-buffer")
                .then().body(Matchers.equalTo("VERTX-BUFFER"));
    }

    @Test
    public void testAsync() {
        RestAssured.get("/simple/async/cs/ok")
                .then().body(Matchers.equalTo("CS-OK"));
        RestAssured.get("/simple/async/cs/fail")
                .then().body(Matchers.equalTo("OK"))
                .statusCode(666);
        RestAssured.get("/simple/async/uni/ok")
                .then().body(Matchers.equalTo("UNI-OK"));
        RestAssured.get("/simple/async/uni/fail")
                .then().body(Matchers.equalTo("OK"))
                .statusCode(666);
    }

    @Test
    public void testMultiResourceSamePath() {
        RestAssured.get("/a")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("a"));
        RestAssured.get("/b")
                .then()
                .statusCode(200)
                .body(Matchers.equalTo("b"));
    }

    @Test
    public void testRequestAndResponseParams() {
        RestAssured.get("/simple/request-response-params")
                .then()
                .body(Matchers.equalTo("127.0.0.1"))
                .header("dummy", "value");

    }

    @Test
    public void testJaxRsRequest() {
        RestAssured.get("/simple/jax-rs-request")
                .then()
                .body(Matchers.equalTo("GET"));
    }

    @Test
    public void testFeature() {
        RestAssured.get("/simple/feature-mapped-exception")
                .then()
                .statusCode(667);

        Headers headers = RestAssured.get("/simple/feature-filters")
                .then().extract().headers();
        assertThat(headers.getValues("feature-filter-request")).containsOnly("authentication-default");
        assertThat(headers.getValues("feature-filter-response")).containsExactly("high-priority", "normal-priority");
    }

    @Test
    public void testDynamicFeature() {
        Headers headers = RestAssured.get("/simple/dynamic-feature-filters")
                .then().extract().headers();
        assertThat(headers.getValues("feature-filter-request")).containsOnly("authentication-default-low");
        assertThat(headers.getValues("feature-filter-response")).containsExactly("high-priority", "normal-priority",
                "low-priority");
    }

    @Test
    public void testResourceInfo() {
        Headers headers = RestAssured.get("/simple/resource-info")
                .then().extract().headers();
        assertThat(headers.getValues("class-name")).containsOnly("SimpleQuarkusRestResource");
        assertThat(headers.getValues("method-name")).containsOnly("resourceInfo");
    }
}
