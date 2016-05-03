package it.gmariotti.recyclerview.itemanimator.demo.models;

/**
 * Created by edmond on 29/04/16.
 */
public class UpdateListEvent {
    public Float itemHeight;
    public Integer position;
    public boolean isNew;

    public UpdateListEvent(Float itemHeight, Integer position, boolean isNew) {
        this.itemHeight = itemHeight;
        this.position = position;
        this.isNew = isNew;
    }
}
