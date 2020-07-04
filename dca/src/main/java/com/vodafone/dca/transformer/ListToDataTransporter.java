package com.vodafone.dca.transformer;

import java.util.List;

import com.vodafone.dca.domain.DataTransporter;

public interface ListToDataTransporter<T> {
	DataTransporter buildFromList(List<T> objects);
}
