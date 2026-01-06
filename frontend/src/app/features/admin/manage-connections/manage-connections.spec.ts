import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ManageConnections } from './manage-connections';

describe('ManageConnections', () => {
  let component: ManageConnections;
  let fixture: ComponentFixture<ManageConnections>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ManageConnections]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ManageConnections);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
