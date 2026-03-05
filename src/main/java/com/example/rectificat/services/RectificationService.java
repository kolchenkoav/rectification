package com.example.rectificat.services;
import com.example.rectificat.model.InData;
import com.example.rectificat.model.OutData;

import java.util.List;

public interface RectificationService {
    OutData calc(InData inData);
    StringBuilder resultToString();
    List<String> resultToStringForHtml();
}
