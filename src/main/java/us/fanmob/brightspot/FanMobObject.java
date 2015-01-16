package us.fanmob.brightspot;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.StringUtils;

public interface FanMobObject extends Recordable {

    /**
     * Implement create to create object via
     * FanMob API. Should set relevant status
     * fields.
     */
    public void create();

    public static class Status extends Modification<FanMobObject> {

        private transient String error;

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public void beforeSave() {

            if (!this.getOriginalObject().getState().isNew()) {
                throw new IllegalArgumentException("FanMob objects cannot be edited.");
            }

            if (!StringUtils.isBlank(this.getError())) {
                throw new IllegalArgumentException(this.getError());
            }

        }

        @Override
        public void afterSave() {

            FanMobObject fanMobObject = this.getOriginalObject();

            fanMobObject.create();

            if (!StringUtils.isBlank(this.getError()) && fanMobObject instanceof Record) {
                ((Record)fanMobObject).save();
            }

        }
    }
}
