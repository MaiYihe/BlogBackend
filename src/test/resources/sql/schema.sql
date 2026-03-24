CREATE TABLE IF NOT EXISTS maihehe_topic (
  id VARCHAR(64),
  path VARCHAR(1024),
  name VARCHAR(255),
  category VARCHAR(255),
  visible BOOLEAN,
  updated_time TIMESTAMP
);

CREATE TABLE IF NOT EXISTS maihehe_note (
  id VARCHAR(64),
  topic_path VARCHAR(1024),
  current_path VARCHAR(2048),
  name VARCHAR(255),
  parent_path VARCHAR(2048),
  is_folder BOOLEAN,
  visible BOOLEAN,
  view_count INT,
  updated_time TIMESTAMP,
  file_mtime BIGINT,
  file_size BIGINT,
  content_hash VARCHAR(64)
);

CREATE TABLE IF NOT EXISTS maihehe_user (
  id INT,
  username VARCHAR(255),
  password VARCHAR(255),
  nickname VARCHAR(255),
  roles VARCHAR(255)
);
