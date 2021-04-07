package com.example.smart.model.response;

import java.util.List;

public class PathResponseVO {
    private String item;
    private List<CoordsVO> path;

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public List<CoordsVO> getPath() {
        return path;
    }

    public void setPath(List<CoordsVO> path) {
        this.path = path;
    }
}
