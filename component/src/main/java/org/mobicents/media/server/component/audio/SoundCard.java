/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.mobicents.media.server.component.audio;

import java.io.IOException;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;

import org.apache.log4j.Logger;
/**
 *
 * @author kulikov
 */
public class SoundCard extends AbstractSink {
    
    private final static AudioFormat LINEAR = FormatFactory.createAudioFormat("LINEAR", 8000, 8, 1);
    private final static Formats formats = new Formats();

    private final static Encoding GSM_ENCODING = new Encoding("GSM0610");
    
    static{
        formats.add(LINEAR);
    }

    private boolean first;
    private SourceDataLine sourceDataLine = null;
    private javax.sound.sampled.AudioFormat audioFormat = null;
    
    private static final Logger logger = Logger.getLogger(SoundCard.class);
    
    public SoundCard(Scheduler scheduler) {
        super("soundcard",scheduler,scheduler.MIXER_INPUT_QUEUE);
    }

    @Override
    public void start() {
        first = true;
        super.start();
    }
    
    @Override
    public void onMediaTransfer(Frame frame) throws IOException {
        System.out.println("Receive " + frame.getFormat() + ", len=" + frame.getLength() + ", header=" + frame.getHeader());
        if (first) {
            first = false;

            AudioFormat fmt = (AudioFormat) frame.getFormat();
            
            if (fmt == null) {
                return;
            }
            
            float sampleRate = (float) fmt.getSampleRate();
            int sampleSizeInBits = fmt.getSampleSize();
            int channels = fmt.getChannels();
            int frameSize = (fmt.getSampleSize() / 8);
            float frameRate = 1;
            boolean bigEndian = false;
            
            Encoding encoding = getEncoding(fmt.getName().toString());

            frameSize = (channels == AudioSystem.NOT_SPECIFIED || sampleSizeInBits == AudioSystem.NOT_SPECIFIED) ? AudioSystem.NOT_SPECIFIED
                    : ((sampleSizeInBits + 7) / 8) * channels;

            audioFormat = new javax.sound.sampled.AudioFormat(encoding, sampleRate, sampleSizeInBits, channels,
                    frameSize, sampleRate, bigEndian);

            // FIXME : Need a configuration to select the specific hardware
            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, audioFormat);

            // TODO : Should getting the SourceDataLine go in start() In which case its configurable to know the Formats
            // beforehand.
            try {
                sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                sourceDataLine.open(audioFormat);
                sourceDataLine.start();

            } catch (Exception e) {
                this.stop();
                logger.error(e);
            }
        }

        // FIXME : write() will block till all bytes are written. Need async operation here.
        byte[] data = frame.getData();
        try {
            sourceDataLine.write(data, frame.getOffset(), frame.getLength());
        } catch (RuntimeException e) {
        	logger.error(e);
        }           
    }    
    
    private javax.sound.sampled.AudioFormat.Encoding getEncoding(String encodingName) {
        if (encodingName.equalsIgnoreCase("pcma")) {
            return javax.sound.sampled.AudioFormat.Encoding.ALAW;
        } else if (encodingName.equalsIgnoreCase("pcmu")) {
            return javax.sound.sampled.AudioFormat.Encoding.ULAW;
        } else if (encodingName.equalsIgnoreCase("gsm")) {
            return GSM_ENCODING;
        } else {
            return javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
        }
    }

    
}
