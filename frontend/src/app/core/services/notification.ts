import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { ConsumerService } from './consumer';
import { AuthService } from './auth';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {
  private unreadCountSubject = new BehaviorSubject<number>(0);
  public unreadCount$ = this.unreadCountSubject.asObservable();
  private totalWorkloadSubject = new BehaviorSubject<number>(0);
  public totalWorkloadCount$ = this.totalWorkloadSubject.asObservable();
  private dropdownListSubject = new BehaviorSubject<any[]>([]);
  public dropdownList$ = this.dropdownListSubject.asObservable();
  private notificationsSubject = new BehaviorSubject<any[]>([]);
  public notifications$ = this.notificationsSubject.asObservable();
  private readNotificationIds: Set<string> = new Set();
  constructor(
    private consumerService: ConsumerService,
    private authService: AuthService
  ) {
    this.loadReadState();
    this.refreshNotifications();
  }
  private loadReadState() {
    const savedIds = localStorage.getItem('readNotifications');
    if (savedIds) {
      this.readNotificationIds = new Set(JSON.parse(savedIds));
    }
  }
  markAsRead(id: string) {
    if (id && !this.readNotificationIds.has(id)) {
      this.readNotificationIds.add(id);
      localStorage.setItem('readNotifications', JSON.stringify([...this.readNotificationIds]));
      this.refreshNotifications(); 
    }
  }
  refreshNotifications() {
    const user = this.authService.getUserFromStorage();
    if (!user) return;
    const roles = user.roles ? user.roles : (user.role ? [user.role] : []);
    const canViewPending = roles.includes('ROLE_BILLING_OFFICER') || roles.includes('ROLE_ADMIN');
    if (canViewPending) {
      this.consumerService.getPendingConnections().subscribe({
        next: (requests) => {
          const allPending = Array.isArray(requests) ? requests : [];          
          this.totalWorkloadSubject.next(allPending.length);
          const unreadPending = allPending.filter(req => !this.readNotificationIds.has(req.id));
          this.unreadCountSubject.next(unreadPending.length);
          this.dropdownListSubject.next(unreadPending);
        },
        error: (err) => console.error('Failed to fetch officer notifications', err)
      });
    } else {
      this.totalWorkloadSubject.next(0);
      this.unreadCountSubject.next(0);
    }
  }
  updateConsumerNotifications(notifs: any[]) {
    this.notificationsSubject.next(notifs);
  }
}