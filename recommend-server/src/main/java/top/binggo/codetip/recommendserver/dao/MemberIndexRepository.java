package top.binggo.codetip.recommendserver.dao;

import codetip.commons.bean.MemberIndex;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;

/**
 * @author binggo
 */
public interface MemberIndexRepository {
    @Query("{\n" +
            "  \"bool\" : {\n" +
            "    \"must\" : [\n" +
            "      {\n" +
            "        \"match\" : {\n" +
            "          \"indexField\" : {\n" +
            "            \"query\" : \"?0\"\n" +
            "            }\n" +
            "        }\n" +
            "      }\n" +
            "    ],\n" +
            "    \"filter\" : [\n" +
            "      {\n" +
            "        \"match\" : {\n" +
            "          \"type\" : {\n" +
            "            \"query\" : \"CLASS\"\n" +
            "            }\n" +
            "        }\n" +
            "      }\n" +
            "    ]}\n" +
            "}")
    Page<MemberIndex> searchClass(String query, Pageable pageable);
}
