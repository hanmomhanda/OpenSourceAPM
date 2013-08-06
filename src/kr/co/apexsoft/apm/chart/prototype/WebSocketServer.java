package kr.co.apexsoft.apm.chart.prototype;

/*
 * Copyright 2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Set;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.ServerWebSocket;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/**
 * 웹소켓을 처리하는 웹서버 클래스
 * 
 * 브라우저로부터 HTTP요청을 받아 웹페이지를 회신해주고
 * 웹소켓으로 업그레이드 하여
 * 웹소켓을 통해 브라우저 전송 정보를 받아
 * 웹소켓을 통해 연결된 모든 브라우저에게 publish한다. 
 * 
 * 소켓서버가 브라우저의 요청을 받아 처리하므로
 * 본 클래스는 소켓서버 에서 구동되어야 한다. 
 * 
 * 실행 스크립트
 *   클래스 파일 Root 디렉토리에서 'vertx run'을 통해 실행
 *   예) 
 *   bin>vertx run fids.websocket.publish.WebSocketServer
 *   
 * 동작 확인 
 *   브라우저를 여러개 띄워 주소창에 127.0.0.1:8080을 입력하고,
 *   하나의 브라우저 에서 'Send Web Socket Data' 버튼을 통해 서버로 발송한 메시지가
 *   서버에 연결된 다른 모든 브라우저에 표시(Publish)되는 것을 확인한다.   
 *
 */
public class WebSocketServer extends Verticle {
	
	final String dataReceiverID = "receiver.websockeserver"; //$NON-NLS-1$
	
	EventBus eb = null;
	
	Set<String> connections = null;

	public void start() {
		
		System.out.println("WebSocketServer verticle started : " + this); //$NON-NLS-1$
		System.out.println("WebSocketServer verticle is in the vertx instance : " + vertx); //$NON-NLS-1$
	
		eb = vertx.eventBus();
		System.out.println("eb of WebSocketServer verticle : " + eb); //$NON-NLS-1$
		
		connections = vertx.sharedData().getSet("conns"); //$NON-NLS-1$
		
		final Verticle currentVerticle = this;				
		
		/**
		 * eventBus에서 데이타를 받아오는 핸들러
		 */
		Handler<Message<String>> msgReceiveHandler = new Handler<Message<String>>() {

			@Override
			public void handle(Message<String> msg) {
				
				publishToWebSocketClient(msg);
			}
			
		};
		eb.registerHandler(dataReceiverID, msgReceiveHandler);		
				
		/**
		 * WebSocket 처리 핸들러
		 */
		Handler<ServerWebSocket> serverWebsocketHandler = new Handler<ServerWebSocket>() {
			
			public void handle(final ServerWebSocket ws) {
				
				connections.add(ws.textHandlerID());				
				
				if (ws.path().equals("/myapp")) { //$NON-NLS-1$
					System.out.println("Verticle for " + ws.textHandlerID() + " : " + currentVerticle); //$NON-NLS-1$ //$NON-NLS-2$
					System.out.println("[in ServerWebSocketHander]ws : " + ws); //$NON-NLS-1$
					System.out.println("[in ServerWebSocketHander]ws.path : " + ws.path()); //$NON-NLS-1$
					
					/**
					 * Websocket으로 데이터 수신 시 처리 핸들러
					 */
					ws.dataHandler(new Handler<Buffer>() {
						
						public void handle(Buffer data) {
							
							// 모든 웹소켓에 반복
							for ( String actorID : connections ) {
								System.out.println("[in ServerWebSocketHander.bufferHandler]--------"); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]ws: " + ws); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]data from client: " + data); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]actorID : " + actorID); //$NON-NLS-1$
								
								// 웹소켓 커넥션에 정보 발송
								eb.publish(actorID, data.toString());
							}

							System.out.println("[in ServerWebSocketHander.bufferHandler]connections : " + connections.size()); //$NON-NLS-1$
			            }
			        });					
					
					/**
					 * WebSocket 정상 종료 시 핸들러
					 * Client Browser 종료 시 여기를 탄다.
					 */
					ws.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void arg0) {
							System.out.println("[in ServerWebSocketHander.endHandler]Connection ends : " + ws.textHandlerID()); //$NON-NLS-1$
							if (connections.remove(ws.textHandlerID())) {
								System.out.println("[in ServerWebSocketHander.endHandler]Connection size : " + connections.size());	 //$NON-NLS-1$
							}							
						}
						
					});
					
			    } else if (ws.path().equals("/data")) { //$NON-NLS-1$
					System.out.println("Verticle for " + ws.textHandlerID() + " : " + currentVerticle); //$NON-NLS-1$ //$NON-NLS-2$
					System.out.println("[in ServerWebSocketHander]ws : " + ws); //$NON-NLS-1$
					System.out.println("[in ServerWebSocketHander]ws.path : " + ws.path()); //$NON-NLS-1$
					
					/**
					 * Websocket으로 데이터 수신 시 처리 핸들러
					 */
					ws.dataHandler(new Handler<Buffer>() {
						
						public void handle(Buffer data) {
							
							// 모든 웹소켓에 반복
							for ( String actorID : connections ) {
								System.out.println("[in ServerWebSocketHander.bufferHandler]--------"); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]ws: " + ws); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]data from client: " + data); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]actorID : " + actorID); //$NON-NLS-1$
								
								// 웹소켓 커넥션에 정보 발송
								eb.publish(actorID, data.toString());
							}

							System.out.println("[in ServerWebSocketHander.bufferHandler]connections : " + connections.size()); //$NON-NLS-1$
			            }
			        });					
					
					/**
					 * WebSocket 정상 종료 시 핸들러
					 * Client Browser 종료 시 여기를 탄다.
					 */
					ws.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void arg0) {
							System.out.println("[in ServerWebSocketHander.endHandler]Connection ends : " + ws.textHandlerID()); //$NON-NLS-1$
							if (connections.remove(ws.textHandlerID())) {
								System.out.println("[in ServerWebSocketHander.endHandler]Connection size : " + connections.size());	 //$NON-NLS-1$
							}							
						}
						
					});
					
			    } else if (ws.path().equals("/jsonData")) { //$NON-NLS-1$
					System.out.println("Verticle for " + ws.textHandlerID() + " : " + currentVerticle); //$NON-NLS-1$ //$NON-NLS-2$
					System.out.println("[in ServerWebSocketHander]ws : " + ws); //$NON-NLS-1$
					System.out.println("[in ServerWebSocketHander]ws.path : " + ws.path()); //$NON-NLS-1$
					
					/**
					 * Websocket으로 데이터 수신 시 처리 핸들러
					 */
					ws.dataHandler(new Handler<Buffer>() {
						
						public void handle(Buffer data) {
							
							// 모든 웹소켓에 반복
							for ( String actorID : connections ) {
								System.out.println("[in ServerWebSocketHander.bufferHandler]--------"); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]ws: " + ws); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]data from client: " + data); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]actorID : " + actorID); //$NON-NLS-1$
								
								// 웹소켓 커넥션에 정보 발송
								eb.publish(actorID, data.toString());
							}

							System.out.println("[in ServerWebSocketHander.bufferHandler]connections : " + connections.size()); //$NON-NLS-1$
			            }
			        });					
					
					/**
					 * WebSocket 정상 종료 시 핸들러
					 * Client Browser 종료 시 여기를 탄다.
					 */
					ws.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void arg0) {
							System.out.println("[in ServerWebSocketHander.endHandler]Connection ends : " + ws.textHandlerID()); //$NON-NLS-1$
							if (connections.remove(ws.textHandlerID())) {
								System.out.println("[in ServerWebSocketHander.endHandler]Connection size : " + connections.size());	 //$NON-NLS-1$
							}							
						}
						
					});
					
			    } else if (ws.path().equals("/chart")) { //$NON-NLS-1$
					System.out.println("Verticle for " + ws.textHandlerID() + " : " + currentVerticle); //$NON-NLS-1$ //$NON-NLS-2$
					System.out.println("[in ServerWebSocketHander]ws : " + ws); //$NON-NLS-1$
					System.out.println("[in ServerWebSocketHander]ws.path : " + ws.path()); //$NON-NLS-1$
					
					/**
					 * Websocket으로 데이터 수신 시 처리 핸들러
					 */
					ws.dataHandler(new Handler<Buffer>() {
						
						public void handle(Buffer data) {
							
							// 모든 웹소켓에 반복
							for ( String actorID : connections ) {
								System.out.println("[in ServerWebSocketHander.bufferHandler]--------"); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]ws: " + ws); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]data from client: " + data); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]actorID : " + actorID); //$NON-NLS-1$
								
								// 웹소켓 커넥션에 정보 발송
//								eb.publish(actorID, data.toString());
							}

							System.out.println("[in ServerWebSocketHander.bufferHandler]connections : " + connections.size()); //$NON-NLS-1$
			            }
			        });					
					
					/**
					 * WebSocket 정상 종료 시 핸들러
					 * Client Browser 종료 시 여기를 탄다.
					 */
					ws.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void arg0) {
							System.out.println("[in ServerWebSocketHander.endHandler]Connection ends : " + ws.textHandlerID()); //$NON-NLS-1$
							if (connections.remove(ws.textHandlerID())) {
								System.out.println("[in ServerWebSocketHander.endHandler]Connection size : " + connections.size());	 //$NON-NLS-1$
							}							
						}
						
					});
					
			    } else if (ws.path().equals("/jsonChart")) { //$NON-NLS-1$
					System.out.println("Verticle for " + ws.textHandlerID() + " : " + currentVerticle); //$NON-NLS-1$ //$NON-NLS-2$
					System.out.println("[in ServerWebSocketHander]ws : " + ws); //$NON-NLS-1$
					System.out.println("[in ServerWebSocketHander]ws.path : " + ws.path()); //$NON-NLS-1$
					
					/**
					 * Websocket으로 데이터 수신 시 처리 핸들러
					 */
					ws.dataHandler(new Handler<Buffer>() {
						
						public void handle(Buffer data) {
							
							// 모든 웹소켓에 반복
							for ( String actorID : connections ) {
								System.out.println("[in ServerWebSocketHander.bufferHandler]--------"); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]ws: " + ws); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]data from client: " + data); //$NON-NLS-1$
								System.out.println("[in ServerWebSocketHander.bufferHandler]actorID : " + actorID); //$NON-NLS-1$
								
								// 웹소켓 커넥션에 정보 발송
//								eb.publish(actorID, data.toString());
							}

							System.out.println("[in ServerWebSocketHander.bufferHandler]connections : " + connections.size()); //$NON-NLS-1$
			            }
			        });					
					
					/**
					 * WebSocket 정상 종료 시 핸들러
					 * Client Browser 종료 시 여기를 탄다.
					 */
					ws.endHandler(new Handler<Void>() {

						@Override
						public void handle(Void arg0) {
							System.out.println("[in ServerWebSocketHander.endHandler]Connection ends : " + ws.textHandlerID()); //$NON-NLS-1$
							if (connections.remove(ws.textHandlerID())) {
								System.out.println("[in ServerWebSocketHander.endHandler]Connection size : " + connections.size());	 //$NON-NLS-1$
							}							
						}
						
					});
					
			    } else {
			        ws.reject();
			    }
			}
		};
		
		/**
		 * HTTP Request 처리 핸들러
		 */
		Handler<HttpServerRequest> httpServerRequestHandler = new Handler<HttpServerRequest>() {			
 
			// websocket이 열린 후부터는 HTTP 요청이 아닌 websocket 요청이므로
			// 본 메서드는 새로운 브라우저가 최초로 HTTP요청을 보냈을 때만 호출되며,
			public void handle(HttpServerRequest req) {
		    	
				System.out.println("[in httpServerReqHandler]------------------------------------------------------"); //$NON-NLS-1$
	        	System.out.println("[in httpServerReqHandler]req from a new browser"); //$NON-NLS-1$
	        	System.out.println("req path : " + req.path());
	        	System.out.println("req uri : " + req.uri());
	        	System.out.println("req method : " + req.method());
	        	
	        	String appRoot = "./kr/co/apexsoft/apm/chart/prototype";
				
	        	if (req.path().equals("/")) { //$NON-NLS-1$
		        	req.response().sendFile(appRoot + "/websocket-test.html"); //$NON-NLS-1$
	        	} else if (req.path().equals("/data")) { //$NON-NLS-1$
		        	req.response().sendFile(appRoot + "/data-provider.html"); //$NON-NLS-1$
		        } else if (req.path().equals("/chart")) { //$NON-NLS-1$
		        	req.response().sendFile(appRoot + "/flotchart.html"); //$NON-NLS-1$
		        } else if (req.path().equals("/shit")) { //$NON-NLS-1$
		        	req.response().sendFile(appRoot + "/shit.html"); //$NON-NLS-1$
		        } else if (req.path().equals("/jsonData")) { //$NON-NLS-1$
		        	req.response().sendFile(appRoot + "/json-provider.html"); //$NON-NLS-1$
		        } else if (req.path().equals("/jsonChart")) { //$NON-NLS-1$
		        	req.response().sendFile(appRoot + "/flotchart-json.html"); //$NON-NLS-1$
		        } else {
		        	req.response().sendFile(appRoot + req.path());
		        }
		    }
		};		
	
		HttpServer httpServer = vertx.createHttpServer();

		// requestHandler와 websocketHandler의 호출 순서는 무관
		httpServer.requestHandler(httpServerRequestHandler).websocketHandler(serverWebsocketHandler).listen(18001);		
	}
	
	/**
	 * msgReceiveHandler가 받아온 데이타를 
	 * 연결되어있는 WebSocket 클라이언트에 뿌려주는 메서드
	 */
	private int publishToWebSocketClient(Message<String> msg) {
		int result = -1;
		
		// 모든 웹소켓에 반복
		for ( String actorID : connections ) {
			System.out.println("[Publishing Msg from C-Client to WebSocketClient]--------"); //$NON-NLS-1$			
			System.out.println("[in ServerWebSocketHander.bufferHandler]actorID : " + actorID); //$NON-NLS-1$
			
			// 웹소켓 커넥션에 정보 발송
			eb.publish(actorID, msg.body());
		}
		
		return result;
	}
}
