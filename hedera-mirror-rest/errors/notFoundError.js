// SPDX-License-Identifier: Apache-2.0

import RestError from './restError';

const NotFoundErrorMessage = 'Not found';

class NotFoundError extends RestError {
  constructor(errorMessage) {
    super(errorMessage === undefined ? NotFoundErrorMessage : errorMessage);
  }
}

export default NotFoundError;
