use android_logger::{Config, log};
use jni::JNIEnv;
use jni::objects::{JClass, JObject, JString, JValue};
use log::LevelFilter;
use nix::libc::read;
use pnet::packet;
use pnet::packet::Packet;
use pnet::packet::ip::{IpNextHeaderProtocol, IpNextHeaderProtocols};
use pnet::packet::ipv4::Ipv4Packet;
use pnet::packet::tcp::TcpPacket;
use std::sync::{Arc, Mutex};
use std::time::Duration;
use tokio::runtime::Runtime;
use tokio::time::sleep;

struct Stream<'a> {
    jni_env: Arc<Mutex<JNIEnv<'a>>>,
    j_object: Arc<Mutex<JObject<'a>>>,
}

impl<'a> Stream<'a> {
    pub fn from_j(jni_env: JNIEnv<'a>, stream_obj: JObject<'a>) -> Stream<'a> {
        Self {
            jni_env: Arc::new(Mutex::new(jni_env)),
            j_object: Arc::new(Mutex::new(stream_obj)),
        }
    }

    pub async fn read(&self, buffer: &mut Vec<u8>) -> std::io::Result<i32> {
        let mut jni_env = self.jni_env.lock().unwrap();
        let mut j_stream_obj = self.j_object.lock().unwrap();

        let jbyte_array = jni_env.byte_array_from_slice(&buffer).unwrap();

        let bytes_read = jni_env.call_method(
            &*j_stream_obj,
            "read",
            "([B)I",
            &[JValue::Object(&jbyte_array)],
        );

        match bytes_read {
            Ok(val) => match val.i() {
                Ok(value) => match jni_env.convert_byte_array(jbyte_array) {
                    Ok(data) => {
                        *buffer = data;
                        Ok(value.into())
                    }
                    Err(error) => Err(std::io::Error::other(error)),
                },
                Err(error) => {
                    log::error!("Failed to convert returned value. Error: {}", error);
                    Err(std::io::Error::other(error))
                }
            },
            Err(e) => {
                log::error!("Failed to read bytes: {}", e);
                Err(std::io::Error::other(e))
            }
        }
    }
}

fn parse_ipv4(packet: Vec<u8>) {
    if let Some(packet) = Ipv4Packet::new(&packet) {
        match packet.get_next_level_protocol() {
            IpNextHeaderProtocols::Tcp => {
                let tcp_packet = TcpPacket::new(packet.payload());

                if let Some(tcp_packet) = tcp_packet {
                    log::debug!("Destination: {:?}", tcp_packet.get_destination());
                }
            }
            IpNextHeaderProtocols::Udp => {}
            _ => {}
        }
    }
}

#[unsafe(no_mangle)]
pub extern "C" fn Java_rahul_secretcodes_vvpn_RustBridge_process(
    env: JNIEnv,
    class: JClass,
    j_stream: JObject,
) {
    init_logging();
    let stream = Stream::from_j(env, j_stream);

    // Create a Tokio runtime to run async tasks
    let rt = Runtime::new().unwrap();
    rt.block_on(async move {
        loop {
            let mut buffer = vec![0; 1024];
            let read_size = match stream.read(&mut buffer).await {
                Ok(read_size) => read_size,
                Err(error) => {
                    log::error!("Failed to read buffer. Error: {}", error);
                    break;
                }
            };

            log::debug!("Read {} bytes", read_size);

            if read_size == 0 {
                sleep(Duration::from_millis(500)).await;
            }

            log::debug!("{:?}", buffer);

            let first_byte = buffer[0].clone();
            if first_byte >> 4 == 4 {
                log::debug!("Got IPV4 packet");
                parse_ipv4(buffer);
            } else if first_byte >> 4 == 6 {
                log::debug!("Got IPV6 packet");
            }
        }
    });
}

fn init_logging() {
    android_logger::init_once(Config::default().with_max_level(LevelFilter::Trace));
}
