package com.guicedee.activitymaster.geography.services.dto;

import com.guicedee.activitymaster.geography.services.enumerations.GeographyFeatureClassesClassifications;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(of = "code")
public class GeographyFeatureCode implements Serializable {

    private String code;
    private String description;

    private GeographyFeatureClassesClassifications classClassification;

    public String toString()
    {
        return code + " - " + description;
    }

    public GeographyFeatureCode setCode(String code)
    {
        this.code = code;
        try
        {
            classClassification = GeographyFeatureClassesClassifications.valueOf(code.charAt(0) + "");
        }catch (Exception e)
        {
            System.out.println("Invalid Feature Code, no Class found " + code);
            e.printStackTrace();
        }
        return this;
    }
}
