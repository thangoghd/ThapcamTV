package com.thangoghd.thapcamtv.response;

import com.thangoghd.thapcamtv.models.Replay;

import java.util.ArrayList;
import java.util.List;

public class ReplayResponse {
    private int status;
    private Data data;

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public Data getData() { return data; }
    public void setData(Data data) { this.data = data; }

    public static class Data {
        private int page;
        private int limit;
        private int total;
        private Replay highlight;
        private List<Replay> list;

        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }

        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }

        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }

        public Replay getHighlight() { return highlight; }
        public void setHighlight(Replay replay) { this.highlight = replay; }

        public List<Replay> getList() {
            List<Replay> combinedList = new ArrayList<>();
            if (highlight != null) {
                combinedList.add(highlight);
            }
            combinedList.addAll(list);
            return combinedList;
        }
    }
}