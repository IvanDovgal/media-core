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

package org.mobicents.media.server.impl.rtp;

import org.mobicents.media.server.impl.rtp.sdp.RTPFormats;
import java.io.IOException;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.text.Format;
import org.mobicents.media.MediaSink;
import org.mobicents.media.MediaSource;
import org.mobicents.media.server.impl.AbstractSink;
import org.mobicents.media.server.impl.AbstractSource;
import org.mobicents.media.server.impl.rtp.sdp.RTPFormat;
import org.mobicents.media.server.io.network.ProtocolHandler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.spi.FormatNotSupportedException;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.format.AudioFormat;
import org.mobicents.media.server.spi.format.FormatFactory;
import org.mobicents.media.server.spi.format.Formats;
import org.mobicents.media.server.spi.memory.Frame;
import org.mobicents.media.server.spi.dsp.Codec;
import org.mobicents.media.server.spi.dsp.Processor;
import org.apache.log4j.Logger;
/**
 *
 * @author Oifa Yulian
 */
/**
 * Receiver implementation.
 *
 * The Media source of RTP data.
 */
public class RTPInput extends AbstractSource implements BufferListener {
	private AudioFormat format = FormatFactory.createAudioFormat("LINEAR", 8000, 16, 1);	
    
    //jitter buffer
    private JitterBuffer rxBuffer;
    
	//digital signaling processor
    private Processor dsp;
           
    protected Integer preEvolveCount=0;
    protected Integer evolveCount=0;
	/**
     * Creates new receiver.
     */
    protected RTPInput(Scheduler scheduler,JitterBuffer rxBuffer) {
        super("input", scheduler,scheduler.INPUT_QUEUE);
        this.rxBuffer=rxBuffer;        
    }

    @Override
    public void reset() {
        super.reset();        
    }
    
    /**
     * Assigns the digital signaling processor of this component.
     * The DSP allows to get more output formats.
     *
     * @param dsp the dsp instance
     */
    public void setDsp(Processor dsp) {
        //assign processor
        this.dsp = dsp;        
    }
    
    /**
     * Gets the digital signaling processor associated with this media source
     *
     * @return DSP instance.
     */
    public Processor getDsp() {
        return this.dsp;
    }
    
    protected int getPacketsLost() {
        return 0;
    }    

    @Override
    public Frame evolve(long timestamp) {
    	Frame currFrame=rxBuffer.read(timestamp);
    	
    	if(currFrame!=null)
        {
    		//do the transcoding job
        	if (dsp != null) {
        		try
        		{
        			currFrame = dsp.process(currFrame,currFrame.getFormat(),format);
        		}
        		catch(Exception e)
        		{
        			//transcoding error , print error and try to move to next frame
        			System.out.println(e.getMessage());
        			e.printStackTrace();
        		}
        	}
        	
        }
    	
    	return currFrame; 
    }    
    
    /**
     * RX buffer's call back method.
     * 
     * This method is called when rxBuffer is full and it is time to start
     * transmission to the consumer.
     */
    public void onFill() {
    	this.wakeup();
    }    
}