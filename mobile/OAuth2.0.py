#!/usr/bin/python3

from oauth2client.service_account import ServiceAccountCredentials

def _get_access_token():
  """Retrieve a valid access token that can be used to authorize requests.

  :return: Access token.
  """
  credentials = ServiceAccountCredentials.from_json_keyfile_name(
      'car-id-55917-601a5782f3a2.json', 'https://www.googleapis.com/auth/firebase.messaging')
  access_token_info = credentials.get_access_token()
  return access_token_info.access_token

token = _get_access_token()
print("ACCESS TOKEN: " + token)
