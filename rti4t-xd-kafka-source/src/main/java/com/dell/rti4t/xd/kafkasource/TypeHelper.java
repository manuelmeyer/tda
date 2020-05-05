package com.dell.rti4t.xd.kafkasource;

public class TypeHelper<T> {
	
	interface InternalCast<T> {
		T internalCast(Object o);
	}
	InternalCast<T> currentCast;
	
	public T transform(Object o) {
		return currentCast.internalCast(o);
	}
	
	@SuppressWarnings("unchecked")
	public TypeHelper(Class<T> clazz) {
		if(Integer.class == clazz) {
			currentCast = (InternalCast<T>) new InternalCast<Integer>() {
				@Override
				public Integer internalCast(Object o) {
					return o == null ? 0 : (Integer)(((Number)o).intValue());
				}
			};
		} else if(Long.class == clazz) {
			currentCast = (InternalCast<T>) new InternalCast<Long>() {
				@Override
				public Long internalCast(Object o) {
					return o == null ? 0 : (Long)(((Number)o).longValue());
				}
			};
		} else if(Double.class == clazz) {
			currentCast = (InternalCast<T>) new InternalCast<Double>() {
				@Override
				public Double internalCast(Object o) {
					return o == null ? 0.0: (Double)o;
				}
			};
		}

	}
}
