package com.guicedee.activitymaster.geography.implementations.graphql;

import com.guicedee.activitymaster.fsdm.client.services.SessionUtils;
import com.guicedee.activitymaster.fsdm.client.services.builders.warehouse.systems.ISystems;
import com.guicedee.activitymaster.geography.services.IGeographyService;
import com.guicedee.activitymaster.geography.services.dto.GeographyCountry;
import com.guicedee.client.IGuiceContext;
import com.guicedee.vertx.graphql.services.IGraphQLSchemaProvider;
import graphql.schema.DataFetcher;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Future;
import org.hibernate.reactive.mutiny.Mutiny;

/**
 * Contributes the strongly-typed {@code GeographyCountry} GraphQL type (and its query) to the shared
 * ActivityMaster GraphQL schema.
 *
 * <p>This provider is discovered automatically (via {@link java.util.ServiceLoader}) whenever the
 * geography module is on the class/module path, so the core service registry exposes the geography
 * data structures without any change to the core itself. The {@code geographyCountry} query is
 * resolved through {@link IGeographyService#findCountryDetailed} inside the canonical
 * {@link SessionUtils#withActivityMaster} security/session context, returning a fully-hydrated
 * {@link GeographyCountry} read straight from the ActivityMaster warehouse.</p>
 *
 * <p>The {@code Query} root is shared with the core {@code FsdmGraphQLSchemaProvider}; this provider
 * therefore {@code extend}s it rather than redefining it.</p>
 */
public class GeographyGraphQLSchemaProvider implements IGraphQLSchemaProvider<GeographyGraphQLSchemaProvider>
{
    private static final String SDL =
            "\"A country as stored within the ActivityMaster geography warehouse.\"\n"
            + "type GeographyCountry {\n"
            + "    \"The warehouse Geography row identifier.\"\n"
            + "    geographyId: String\n"
            + "    \"The original GeoNames identifier, when known.\"\n"
            + "    geonameId: String\n"
            + "    \"ISO-3166 alpha-2 country code.\"\n"
            + "    iso: String\n"
            + "    \"ISO-3166 alpha-3 country code.\"\n"
            + "    iso3: String\n"
            + "    \"ISO-3166 numeric country code.\"\n"
            + "    isoNumeric: String\n"
            + "    \"FIPS country code.\"\n"
            + "    fips: String\n"
            + "    \"Full country name.\"\n"
            + "    countryName: String\n"
            + "    \"Capital city name.\"\n"
            + "    capital: String\n"
            + "    \"Area in square kilometres.\"\n"
            + "    areaSqlKM: String\n"
            + "    \"Top-level internet domain (e.g. .za).\"\n"
            + "    webTld: String\n"
            + "    \"Population count.\"\n"
            + "    population: Int\n"
            + "    \"International dialling code.\"\n"
            + "    countryDialCode: String\n"
            + "    \"Postal code decimal format mask.\"\n"
            + "    postalCodeDecimalFormat: String\n"
            + "    \"Postal code validation regular expression.\"\n"
            + "    postalCodeRegexFormat: String\n"
            + "}\n"
            + "\n"
            + "extend type Query {\n"
            + "    \"Resolves a single country by its ISO-3166 alpha-2 code within an enterprise/system scope.\"\n"
            + "    geographyCountry(enterprise: String!, system: String!, iso: String!): GeographyCountry\n"
            + "}\n";

    @Override
    public TypeDefinitionRegistry getTypeDefinitions()
    {
        return new SchemaParser().parse(SDL);
    }

    @Override
    public RuntimeWiring.Builder configureWiring(RuntimeWiring.Builder builder)
    {
        return builder
                .type("Query", q -> q.dataFetcher("geographyCountry", countryFetcher()))
                .type("GeographyCountry", t -> t
                        .dataFetcher("geographyId", env -> {
                            GeographyCountry c = env.getSource();
                            return c == null || c.getGeographyId() == null ? null : c.getGeographyId().toString();
                        })
                        .dataFetcher("geonameId", env -> {
                            GeographyCountry c = env.getSource();
                            return c == null || c.getGeonameId() == null ? null : c.getGeonameId().toString();
                        }));
    }

    /**
     * Builds the data fetcher for the {@code geographyCountry} query. Execution runs inside the
     * canonical Activity Master security/session context and the resulting Mutiny {@link Uni} is
     * bridged to a Vert.x {@link Future} so the auto-installed {@code VertxFutureAdapter} resolves it.
     */
    private DataFetcher<Future<GeographyCountry>> countryFetcher()
    {
        return env -> {
            String enterprise = env.getArgument("enterprise");
            String system = env.getArgument("system");
            String iso = env.getArgument("iso");

            Uni<GeographyCountry> uni = SessionUtils.withActivityMaster(enterprise, system, tuple -> {
                Mutiny.Session session = tuple.getItem1();
                ISystems<?, ?> sys = tuple.getItem3();
                IGeographyService<?> service = IGuiceContext.get(IGeographyService.class);
                return service.findCountryDetailed(session, iso, sys, tuple.getItem4());
            });

            return Future.fromCompletionStage(uni.subscribeAsCompletionStage());
        };
    }
}


