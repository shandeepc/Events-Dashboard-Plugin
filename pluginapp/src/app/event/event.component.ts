import { Component, Input, ViewChild, isDevMode } from '@angular/core';
import { EventFullViewComponent } from '../event-full-view/event-full-view.component';

interface EventDetail {
  targetName: string;
  createdDate: string;
  endDate: string;
  uniqueHash: string;
  taskResultId: string;
  targetDisplayName: string;
  targetId: string;
  requesterDisplayName: string;
  requesterId: string;
  identityRequestId: string;
  type: string;
  identityRequestCompletionStatusString: string;
  taskResultName: string;
  taskResultStatus: string;
}

declare const SailPoint: {
  CONTEXT_PATH: string
};

@Component({
  selector: 'app-event',
  templateUrl: './event.component.html',
  styleUrls: ['./event.component.css']
})
export class EventComponent {
  @Input() eventName!: string;
  @Input() eventData!: EventDetail[];
  @Input() isLoading!: boolean;
  @ViewChild('fullView') fullView!: EventFullViewComponent;

  private iiqUrl: string;

  constructor() {
    if (isDevMode()) {
      this.iiqUrl = 'http://sp.com:8080'; // use proxy
    } else {
      this.iiqUrl = SailPoint.CONTEXT_PATH;
    }
  }

  openIdentityRequest(identityRequestId: string): void {
    const url = `${this.iiqUrl}/identityRequest/identityRequest.jsf#/request/${identityRequestId}`;
    window.open(url, '_blank');
  }

  openIdentity(targetId: string): void {
    const url = `${this.iiqUrl}/define/identity/identity.jsf?id=${targetId}`;
    window.open(url, '_blank');
  }
}
