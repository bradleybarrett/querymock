package com.bbarrett.querymock.wiremock;

import com.bbarrett.querymock.util.FileUtil;
import com.bbarrett.querymock.util.JsonUtil;
import org.mvel2.MVEL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class QueryDataStore
{
    private static Logger logger = LoggerFactory.getLogger(QueryDataStore.class);

    private Map<String, List<Object>> filenameToQueryDataMap = new HashMap<>();

    public QueryDataStore(String directory)
    {
        populateDataFromDirectory(directory);
    }

    public void setDataForEndpoint(String filename, List<Object> data)
    {
        filenameToQueryDataMap.put(filename, data);
    }

    public void populateDataFromDirectory(String directory)
    {
        logger.info("directory: {}", directory);
        FileUtil.getFilesFromDirectory(directory).forEach(file ->
                setDataForEndpoint(file.getName(), JsonUtil.parseJsonListFromFile(file)));
    }

    public Object queryForObject(String filename, String mvelBooleanExpression)
    {
        return executeQuery(filename, mvelBooleanExpression)
                .findFirst()
                .orElse(null);
    }

    public List<Object> queryForList(String filename, String mvelBooleanExpression)
    {
        return executeQuery(filename, mvelBooleanExpression)
                .collect(Collectors.toList());
    }

    private Stream<Object> executeQuery(String filename, String mvelBooleanExpression)
    {
        Serializable expr = MVEL.compileExpression(mvelBooleanExpression);
        List<Object> dataList = filenameToQueryDataMap.get(filename);
        logger.info("dataList: {}", dataList);

        return dataList.stream()
                .filter(car -> MVEL.executeExpression(expr, car, boolean.class));
    }
}
