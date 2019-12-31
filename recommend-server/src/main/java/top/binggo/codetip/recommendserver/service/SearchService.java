package top.binggo.codetip.recommendserver.service;

import codetip.commons.bean.MemberIndex;
import codetip.commons.bean.Page;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.binggo.codetip.recommendserver.bean.MavenLocation;
import top.binggo.codetip.recommendserver.config.AppConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author binggo
 */
@Service
@Slf4j
public class SearchService {

    private final
    PreBuiltTransportClient preBuiltTransportClient;

    private final
    AppConfig appConfig;

    @Autowired
    public SearchService(PreBuiltTransportClient preBuiltTransportClient, AppConfig appConfig) {
        this.preBuiltTransportClient = preBuiltTransportClient;
        this.appConfig = appConfig;
    }

    public Page.Respond<MemberIndex> searchProject(Page.Request page, String keyWords, boolean onlyRecommendVersion) {
        return getInSymbolAndIndexField(new String[]{"PROJECT"}, keyWords, null, null, page, onlyRecommendVersion, false, false);
    }

    public Page.Respond<MemberIndex> searchClass(Page.Request page, MavenLocation mavenLocation, String keyWords, boolean onlyPublicMember) {
        return getInSymbolAndIndexField(new String[]{"CLASS"}, keyWords, mavenLocation, null, page, false, onlyPublicMember, false);
    }

    public Page.Respond<MemberIndex> searchMember(Page.Request page, MavenLocation mavenLocation, String className, String keyWords, boolean filterOverloadMethod, boolean onlyPublicMember) {
        return getInSymbolAndIndexField(new String[]{"METHOD", "FIELD"}, keyWords, mavenLocation, className, page, false, onlyPublicMember, filterOverloadMethod);
    }

    //todo 添加按照使用情况进行排序功能
//todo 解决分页查询不到数据的情况
    @SuppressWarnings("unchecked")
    private Page.Respond<MemberIndex> get(String[] types, String keyWords, MavenLocation mavenLocation, String className, Page.Request page, boolean onlyRecommendVersion, boolean onlyPublicMember, boolean filterOverloadMethod, String searchField) {
        log.info("types = [" + Arrays.toString(types) + "], keyWords = [" + keyWords + "], mavenLocation = [" + mavenLocation + "], className = [" + className + "], page = [" + page + "], onlyRecommendVersion = [" + onlyRecommendVersion + "], onlyPublicMember = [" + onlyPublicMember + "], filterOverloadMethod = [" + filterOverloadMethod + "]");
        SearchRequestBuilder searchRequestBuilder = preBuiltTransportClient.prepareSearch(appConfig.getIndexName()).setSearchType(SearchType.DFS_QUERY_THEN_FETCH)
                .setFrom(page.getFrom())
                .setSize(page.getSize())
                .setFetchSource(null, new String[]{"indexField", "source"});
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (keyWords != null && !keyWords.isEmpty()) {
            queryBuilder.must(QueryBuilders.matchQuery(searchField, keyWords));
        } else {
            queryBuilder.must(QueryBuilders.matchAllQuery());
        }
        if (onlyPublicMember) {
            queryBuilder.filter(QueryBuilders.termQuery("accessLevel", 3));
        }
        if (filterOverloadMethod) {
            //todo 根据方法的symbol进行分组
        }
        if (onlyRecommendVersion) {
            //todo 在下载下来maven-metadata-public.xml文件之后更新对应项目的推荐版本
            queryBuilder.filter(QueryBuilders.termQuery("recommendVersion", true));
        }
        BoolQueryBuilder typeQueryBuilder = QueryBuilders.boolQuery();
        for (String type : types) {
            typeQueryBuilder.should(QueryBuilders.matchQuery("type", type));
        }
        queryBuilder.filter(typeQueryBuilder);
        if (mavenLocation != null && !mavenLocation.isEmpty()) {
            queryBuilder.filter(QueryBuilders.termsQuery("groupId", mavenLocation.getGroupId().split("-")))
                    .filter(QueryBuilders.termsQuery("artifactId", mavenLocation.getArtifactId().split("-")))
                    .filter(QueryBuilders.termsQuery("version", mavenLocation.getVersion().split("-")));
        }
        if (className != null && !className.isEmpty()) {
            queryBuilder.filter(QueryBuilders.matchQuery("className", className));
        }
        // Function 得分：Field值因子（ Function Score: Field Value Factor

        ScoreFunctionBuilder<?> scoreFunctionBuilder = ScoreFunctionBuilders.fieldValueFactorFunction("className").modifier(Modifier.LN1P).factor(0.1f);
        FunctionScoreQueryBuilder query = QueryBuilders.functionScoreQuery(queryBuilder,scoreFunctionBuilder).boostMode(CombineFunction.SUM);

        //Function 得分：衰减函数( Function Score: Decay Functions )
        //原点（origin）：该字段最理想的值，这个值可以得到满分（1.0）
        double origin = 200;
//偏移量（offset）：与原点相差在偏移量之内的值也可以得到满分
        double offset = 30;
//衰减规模（scale）：当值超出了原点到偏移量这段范围，它所得的分数就开始进行衰减了，衰减规模决定了这个分数衰减速度的快慢
        double scale = 40;
//衰减值（decay）：该字段可以被接受的值（默认为 0.5），相当于一个分界点，具体的效果与衰减的模式有关
        double decay = 0.5;
//以 e 为底的指数函数
        ExponentialDecayFunctionBuilder functionBuilder = ScoreFunctionBuilders.exponentialDecayFunction("className", origin, scale, offset, decay);
//线性函数
        FunctionScoreQueryBuilder query = QueryBuilders.functionScoreQuery(queryBuilder,functionBuilder).boostMode(CombineFunction.SUM);



        searchRequestBuilder.setQuery(queryBuilder);
        SearchResponse searchResponse = searchRequestBuilder.get();

        SearchHits hits = searchResponse.getHits();
        SearchHit[] content = hits.getHits();
        List<MemberIndex> ret = new ArrayList<>(content.length);
        for (SearchHit documentFields : content) {
            MemberIndex memberIndex = MemberIndex.fromSearchHit(documentFields);
            ret.add(memberIndex);
        }
        if (filterOverloadMethod) {
            final HashSet<String> strings = new HashSet<>();
            ret = ret.stream().filter(memberIndex -> {
                String symbol = memberIndex.getSymbol();
                boolean contains = strings.contains(symbol);
                strings.add(symbol);
                return !contains;
            }).collect(Collectors.toList());
        }
        return new Page.Respond((int) hits.getTotalHits(), page.getFrom(), hits.getHits().length, ret);
    }

    private Page.Respond<MemberIndex> getInSymbolAndIndexField(String[] types, String keyWords, MavenLocation mavenLocation, String className, Page.Request page, boolean onlyRecommendVersion, boolean onlyPublicMember, boolean filterOverloadMethod) {
        if (keyWords.startsWith("#")) {
            keyWords = keyWords.substring(1);
            return get(types, keyWords, mavenLocation, className, page, onlyRecommendVersion, onlyPublicMember, filterOverloadMethod, "symbol");
        } else {
            return get(types, keyWords, mavenLocation, className, page, onlyRecommendVersion, onlyPublicMember, filterOverloadMethod, "indexField");
        }
    }


}
