db.createCollection('Messages',{
	validator: {
  $jsonSchema: {
    bsonType: 'object',
    required: [
      'roomID',
      'userID',
      'text',
      'timestamp'
    ],
    properties: {
      userID: {
        bsonType: 'string'
      },
      roomID: {
        bsonType: 'objectId'
      },
      text: {
        bsonType: 'string'
      },
      timestamp: {
        bsonType: 'date'
      }
    }
  }
}
})
db.createCollection('User', {
	validator: {
  $jsonSchema: {
    bsonType: 'object',
    required: [
      'nickname',
      'userID'
    ],
    properties: {
      userID: {
        bsonType: 'string'
      },
      nickname: {
        bsonType: 'string'
      }
    }
  }
}
})
db.createCollection('Room', {
	validator: {
  $jsonSchema: {
    bsonType: 'object',
    required: [
      'start_time',
      'participants'
    ],
    properties: {
      start_time: {
        bsonType: 'date'
      },
      participants: {
        bsonType: 'array',
        items: {
          bsonType: 'object'
        },
        required: [
          'userID',
          'idkeys',
          'otkeys'
        ],
        properties: {
          userID: {
            bsonType: 'string'
          },
          idkeys: {
            bsonType: 'string'
          },
          otkeys: {
            bsonType: 'string'
          }
        }
      }
    }
  }
}
})