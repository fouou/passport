package com.fou.passport.digital_asset_links;

import java.util.List;

public class BeanDigitalAssetLinks {

    /**
     * relation : ["delegate_permission/common.handle_all_urls","delegate_permission/common.get_login_creds"]
     * target : {"namespace":"android_app","package_name":"com.facebook.orca","sha256_cert_fingerprints":["E3:F9:E1:E0:CF:99:D0:E5:6A:05:5B:A6:5E:24:1B:33:99:F7:CE:A5:24:32:6B:0C:DD:6E:C1:32:7E:D0:FD:C1"]}
     */

    private TargetDTO target;
    private List<String> relation;

    public TargetDTO getTarget() {
        return target;
    }

    public void setTarget(TargetDTO target) {
        this.target = target;
    }

    public List<String> getRelation() {
        return relation;
    }

    public void setRelation(List<String> relation) {
        this.relation = relation;
    }

    public static class TargetDTO {
        /**
         * namespace : android_app
         * package_name : com.facebook.orca
         * sha256_cert_fingerprints : ["E3:F9:E1:E0:CF:99:D0:E5:6A:05:5B:A6:5E:24:1B:33:99:F7:CE:A5:24:32:6B:0C:DD:6E:C1:32:7E:D0:FD:C1"]
         */

        private String namespace;
        private String package_name;
        private List<String> sha256_cert_fingerprints;

        public String getNamespace() {
            return namespace;
        }

        public void setNamespace(String namespace) {
            this.namespace = namespace;
        }

        public String getPackage_name() {
            return package_name;
        }

        public void setPackage_name(String package_name) {
            this.package_name = package_name;
        }

        public List<String> getSha256_cert_fingerprints() {
            return sha256_cert_fingerprints;
        }

        public void setSha256_cert_fingerprints(List<String> sha256_cert_fingerprints) {
            this.sha256_cert_fingerprints = sha256_cert_fingerprints;
        }

        @Override
        public String toString() {
            return "TargetDTO{" +
                    "namespace='" + namespace + '\'' +
                    ", package_name='" + package_name + '\'' +
                    ", sha256_cert_fingerprints=" + sha256_cert_fingerprints +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "BeanDigitalAssetLinks{" +
                "target=" + target +
                ", relation=" + relation +
                '}';
    }
}
