package codetip.commons.bean;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * @author binggo
 */
public class Page {
    @Data
    @AllArgsConstructor
    public static class Request {
        private int from;
        private int size;
    }

    @Data
    @AllArgsConstructor
    @Builder
    public static class Respond<T> {
        private int totalSize;
        private int from;
        private int size;
        private List<T> data;
    }
}
