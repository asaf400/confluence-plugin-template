package com.groupie.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.validation.annotation.Validated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

public class ConfigModels {
    public ConfigModels() {
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    @Validated
    public static final class LabelConfig {
        @XmlElement
        @JsonProperty(required = true)
        public String label;

        @XmlElement
        @JsonProperty(required = true)
        public List<String> allowedGroups;
    }
}