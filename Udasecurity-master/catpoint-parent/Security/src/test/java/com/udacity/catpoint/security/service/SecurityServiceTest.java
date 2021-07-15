package com.udacity.catpoint.security.service;


import com.udacity.catpoint.image.service.ImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private SecurityService securityService;
    private Sensor sensor;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private StatusListener statusListener;

    private Sensor createNewSensor(){
        return new Sensor("newSensor", SensorType.DOOR);
    }

    @BeforeEach
    void init(){
        securityService = new SecurityService(securityRepository, imageService);
        sensor = createNewSensor();
    }
    private final String random = UUID.randomUUID().toString();
    private Set<Sensor> getAllSensors(int count, boolean status) {
        Set<Sensor> sensors = new HashSet<>();
        for (int i = 0; i < count; i++) {
            sensors.add(new Sensor(random, SensorType.DOOR));
        }
        sensors.forEach(sensor -> sensor.setActive(status));

        return sensors;
    }


    //test 1
    @Test
    public void ifAlarmArmedAndAlarmStatusNoAlarmAndSensorActivated_SetToAlarmStatusPending(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    //tesst 2
    @Test
    public void ifAlarmArmedAndAlarmStatusPendingAndSensorActivated_SetToAlarmStatusAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    //test 3
    @Test
    public void ifAlarmArmedAndAlarmStatusPendingAndSensorNotActivated_SetToAlarmStatusNoAlarm(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //test 4
    @ParameterizedTest
    @ValueSource(booleans = {true})
    void ifAlarmIsActive_changeSensorShouldNotAffectAlarmState(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);

        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // test 5
    @Test
    void ifSensorActivated_andAlreadyActive_andSystemIsInPendingState_changeToAlarmState(){

        when ( securityRepository.getArmingStatus ( ) ).thenReturn ( ArmingStatus.ARMED_HOME );

        when ( securityRepository.getAlarmStatus ( ) ).thenReturn ( AlarmStatus.PENDING_ALARM );

        securityService.changeSensorActivationStatus ( sensor, true );


        verify ( securityRepository ).setAlarmStatus ( AlarmStatus.ALARM );
    }


    // test 6
    @Test
    void checkInactiveOrDeactivatedSensor_noChangeInAlarmState(){
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        sensor.setActive(Boolean.FALSE); //sensor deactivated
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }

    //test 7
    @Test
    void armingStatusArmedHomeAndIdentifiesCat_SetToAlarmStatusAlarm(){
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        BufferedImage imageOfCat = new BufferedImage(300, 225, TYPE_INT_ARGB);
        when(imageService.imageContainsCat(imageOfCat, 50.0f)).thenReturn(Boolean.TRUE);
        securityService.processImage(imageOfCat);
        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }


    @Test
    void imageServiceIdentifiesNotCatAndSensorsDeactivatedSetAlarmStatusNoAlarm(){
        BufferedImage notImageOfCat = new BufferedImage(300, 225, TYPE_INT_ARGB);
        when(imageService.imageContainsCat(notImageOfCat, 50.0f)).thenReturn(Boolean.FALSE);
        securityService.processImage(notImageOfCat);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //test 9
    @Test
    void armingStatusDisarmed_SetToAlarmStatusNoAlarm(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }


    //test 10
    @Test
    void systemArmed_resetAllSensorsToInactive(){
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        securityService.getSensors().forEach(sensor1 -> {
            assert Boolean.FALSE.equals(sensor1.getActive());
        });
    }

    //test 11
    @Test
    void systemArmedHomeWhenImageServiceIdentifiesCatChangeStatusToAlarm() {
        when ( securityRepository.getArmingStatus ( ) ).thenReturn ( ArmingStatus.ARMED_HOME );
        when ( imageService.imageContainsCat ( any ( ), ArgumentMatchers.anyFloat ( ) )).thenReturn ( true );
        securityService.processImage ( mock ( BufferedImage.class ) );

        verify ( securityRepository, times ( 1 ) ).setAlarmStatus ( AlarmStatus.ALARM );
    }

    @Test
    void addAndRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }
    //Sensor Listener test
    @Test
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }

    //final test
    @Test
    void ifAlarmStateAndSystemDisarmed_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }
}