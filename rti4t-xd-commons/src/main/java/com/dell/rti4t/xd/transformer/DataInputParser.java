package com.dell.rti4t.xd.transformer;

import java.util.List;

public interface DataInputParser<T, R> extends ListToDataTransporter<T> {
	List<List<T>> parse(R input);
}
