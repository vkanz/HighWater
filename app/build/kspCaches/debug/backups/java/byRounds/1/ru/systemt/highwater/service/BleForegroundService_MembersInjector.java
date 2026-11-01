package ru.systemt.highwater.service;

import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import ru.systemt.highwater.data.ble.BleManager;

@QualifierMetadata
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
public final class BleForegroundService_MembersInjector implements MembersInjector<BleForegroundService> {
  private final Provider<BleManager> bleManagerProvider;

  public BleForegroundService_MembersInjector(Provider<BleManager> bleManagerProvider) {
    this.bleManagerProvider = bleManagerProvider;
  }

  public static MembersInjector<BleForegroundService> create(
      Provider<BleManager> bleManagerProvider) {
    return new BleForegroundService_MembersInjector(bleManagerProvider);
  }

  @Override
  public void injectMembers(BleForegroundService instance) {
    injectBleManager(instance, bleManagerProvider.get());
  }

  @InjectedFieldSignature("ru.systemt.highwater.service.BleForegroundService.bleManager")
  public static void injectBleManager(BleForegroundService instance, BleManager bleManager) {
    instance.bleManager = bleManager;
  }
}
