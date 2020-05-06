package com.bbarrett.querymock.wiremock.extension;

import com.bbarrett.querymock.util.JsonUtil;
import com.bbarrett.querymock.wiremock.QueryDataStore;
import com.bbarrett.querymock.wiremock.QueryDetails;
import com.github.tomakehurst.wiremock.common.FileSource;
import com.github.tomakehurst.wiremock.extension.Parameters;
import com.github.tomakehurst.wiremock.extension.ResponseTransformer;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.http.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

public class QueryTransformer extends ResponseTransformer
{
    private static Logger logger = LoggerFactory.getLogger(QueryTransformer.class);

    private static final String TRANSFORMER_NAME = "query-transformer";
    private static final boolean APPLY_GLOBALLY = false;
    private static final String RESULT_PLACEHOLDER = "\"{{ result }}\"";

    private QueryDataStore queryDataStore;

    public QueryTransformer(QueryDataStore queryDataStore)
    {
        this.queryDataStore = queryDataStore;
    }

    @Override
    public String getName() {
        return TRANSFORMER_NAME;
    }

    @Override
    public boolean applyGlobally() {
        return APPLY_GLOBALLY;
    }

    @Override
    public Response transform(Request request, Response response, FileSource files, Parameters parameters)
    {
        logger.info("response.body: {}", response.getBodyAsString());

        QueryDetails queryDetails = JsonUtil.getObject(response.getBodyAsString(), QueryDetails.class);

        String filename = queryDetails.getData();
        String mvelBooleanExpression = queryDetails.getQuery();
        boolean findOne = queryDetails.getFindOne();

        String bodyTemplate = RESULT_PLACEHOLDER;
        if (!StringUtils.isEmpty(queryDetails.getBodyTemplate()))
        {
            bodyTemplate = JsonUtil.getJsonString(queryDetails.getBodyTemplate());
        }

        logger.info("filename: {}", filename);
        logger.info("mvelBooleanExpression: {}", mvelBooleanExpression);
        logger.info("findOne: {}", findOne);
        logger.info("bodyTemplate: {}", bodyTemplate);

        Object queryResult;
        if (findOne)
            queryResult = queryDataStore.queryForObject(filename, mvelBooleanExpression);
        else
            queryResult = queryDataStore.queryForList(filename, mvelBooleanExpression);

        String bodyWithResult = null;
        if (queryResult != null)
        {
            bodyWithResult = bodyTemplate.replace(RESULT_PLACEHOLDER, JsonUtil.getJsonString(queryResult));
        }
        logger.info("bodyWithResult: {}", bodyWithResult);

        return Response.Builder.like(response)
                .but().body(bodyWithResult)
                .build();
    }
}


