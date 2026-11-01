package ru.systemt.highwater.service;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class BleServiceManager_Factory implements Factory<BleServiceManager> {
  private final Provider<Context> contextProvider;

  public BleServiceManager_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public BleServiceManager get() {
    return newInstance(contextProvider.get());
  }

  public static BleServiceManager_Factory create(Provider<Context> contextProvider) {
    return new BleServiceManager_Factory(contextProvider);
  }

  public static BleServiceManager newInstance(Context context) {
    return new BleServiceManager(context);
  }
}
